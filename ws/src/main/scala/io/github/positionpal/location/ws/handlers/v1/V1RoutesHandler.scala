package io.github.positionpal.location.ws.handlers.v1

import scala.concurrent.ExecutionContext

import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.actor.typed.{ActorRef, ActorSystem}
import io.bullet.borer.Json
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.typed.scaladsl.ActorSource
import io.github.positionpal.location.ws.handlers.RoutesHandler
import cats.effect.IO
import akka.stream.{Attributes, OverflowStrategy}
import io.github.positionpal.location.tracking.ActorBasedRealTimeTracking
import akka.event.Logging.*
import akka.NotUsed
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.{GroupId, UserId}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.event.Logging
import io.github.positionpal.location.presentation.ModelCodecs

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
    Flow[Message]
      .map:
        case TextMessage.Strict(msg) => Json.decode(msg.getBytes).to[DrivingEvent].valueEither
        case t => Left(s"Unsupported message type $t")
      .log("Websocket message from client to tracker")
      .withAttributes(attributes)
      .collect { case Right(e) => e }
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
    ActorSource
      .actorRef(
        completionMatcher = { case Complete => },
        failureMatcher = { case ex: Throwable => ex },
        bufferSize = 1_000,
        overflowStrategy = OverflowStrategy.dropHead,
      )
      .mapMaterializedValue: ref =>
        val oldConnections = ConnectionsManager.addConnection(scope.group, scope.user, ref)
        oldConnections.foreach((uid, _) => service.addObserverFor(Scope(uid, scope.group))(Set(ref)).unsafeRunSync())
        service.addObserverFor(scope)(oldConnections.map(_._2) + ref).unsafeRunSync()
        ref
      .log("Websocket message from tracker to client")
      .withAttributes(attributes)
      .map:
        case event: DrivenEvent => TextMessage(Json.encode(event).toUtf8String)

  private def attributes = Attributes.logLevels(onFinish = InfoLevel, onFailure = ErrorLevel, onElement = InfoLevel)
