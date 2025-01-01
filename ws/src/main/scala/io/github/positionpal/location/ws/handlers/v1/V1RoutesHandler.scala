package io.github.positionpal.location.ws.handlers.v1

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.event.Logging.*
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{Attributes, OverflowStrategy}
import cats.effect.IO
import io.bullet.borer.Json
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.presentation.ModelCodecs
import io.github.positionpal.location.tracking.ActorBasedRealTimeTracking
import io.github.positionpal.location.ws.handlers.RoutesHandler

/** The handler for the v1 version of the web socket API. */
class V1RoutesHandler(service: ActorBasedRealTimeTracking.Service[IO, Scope])(using actorSystem: ActorSystem[?])
    extends RoutesHandler[[T] =>> Flow[Message, Message, T]]
    with ModelCodecs:

  given ExecutionContext = actorSystem.executionContext
  import cats.effect.unsafe.implicits.global

  override def handleTrackingRoute(userId: UserId, groupId: GroupId): Flow[Message, Message, NotUsed] =
    val scope = Scope(userId, groupId)
    Flow.fromSinkAndSource(routeToGroupActor(scope), routeToClient(scope))

  private def routeToGroupActor(scope: Scope): Sink[Message, Unit] =
    Flow[Message].map:
      case TextMessage.Strict(msg) => Json.decode(msg.getBytes).to[DrivingEvent].valueEither
      case t => Left(s"Unsupported message type $t")
    .log("Websocket message from client to tracker").withAttributes(attributes).collect { case Right(e) => e }
      .watchTermination(): (_, done) =>
        done.onComplete: _ =>
          val oldConnections = ConnectionsManager.removeConnection(scope.group, scope.user)
          val scopeActorRef = oldConnections.find(_._1 == scope.user).map(_._2)
          scopeActorRef.foreach: ref =>
            oldConnections
              .foreach((uid, _) => service.removeObserverFor(Scope(uid, scope.group))(Set(ref)).unsafeRunSync())
            service.removeObserverFor(scope)(oldConnections.map(_._2) - ref).unsafeRunSync()
      .to(Sink.foreach(service.handle(scope)(_).unsafeRunSync()))

  private def routeToClient(scope: Scope): Source[Message, ActorRef[DrivenEvent]] =
    ActorSource.actorRef(
      completionMatcher = { case Complete => },
      failureMatcher = { case ex: Throwable => ex },
      bufferSize = 1_000,
      overflowStrategy = OverflowStrategy.dropHead,
    ).mapMaterializedValue: ref =>
      val oldConnections = ConnectionsManager.addConnection(scope.group, scope.user, ref)
      oldConnections.foreach((uid, _) => service.addObserverFor(Scope(uid, scope.group))(Set(ref)).unsafeRunSync())
      service.addObserverFor(scope)(oldConnections.map(_._2) + ref).unsafeRunSync()
      ref
    .log("Websocket message from tracker to client").withAttributes(attributes).map:
      case event: DrivenEvent => TextMessage(Json.encode(event).toUtf8String)

  private def attributes = Attributes.logLevels(onFinish = InfoLevel, onFailure = ErrorLevel, onElement = InfoLevel)
