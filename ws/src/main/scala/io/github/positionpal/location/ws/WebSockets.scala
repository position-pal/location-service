package io.github.positionpal.location.ws

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global

import akka.NotUsed
import akka.http.scaladsl.server.RouteResult.Complete
import cats.effect.IO
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.tracking.ActorBasedRealTimeTracking
import io.github.positionpal.location.presentation.ModelCodecs

/** Web socket port service implementation for real time tracking. */
object WebSockets:

  /** The routes for the web socket communication. */
  object Routes:

    import akka.http.scaladsl.server.Directives.*
    import akka.http.scaladsl.server.Route

    def groupRoute(service: ActorBasedRealTimeTracking.Service[IO, Scope]): Route =
      path("group" / Segment / Segment): (guid, uid) =>
        handleWebSocketMessages:
          Handlers.handleGroupRoute(UserId.create(uid), GroupId.create(guid), service)

  /** The handlers for the web socket communication. */
  object Handlers extends ModelCodecs:

    import akka.actor.typed.ActorRef
    import akka.http.scaladsl.model.ws.{Message, TextMessage}
    import akka.stream.scaladsl.{Flow, Sink, Source}
    import akka.stream.OverflowStrategy
    import akka.stream.typed.scaladsl.ActorSource
    import cats.effect.unsafe.implicits.global
    import io.bullet.borer.Json

    private val activeSessions = TrieMap[GroupId, Set[(UserId, ActorRef[DrivenEvent])]]()

    def handleGroupRoute(
        userId: UserId,
        groupId: GroupId,
        service: ActorBasedRealTimeTracking.Service[IO, Scope],
    ): Flow[Message, Message, NotUsed] =
      val scope = Scope(userId, groupId)
      val routeToGroupActor: Sink[Message, Unit] = Flow[Message]
        .map:
          case TextMessage.Strict(msg) => Json.decode(msg.getBytes).to[DrivingEvent].valueEither
          case _ => Left("Invalid message")
        .collect { case Right(e) => e }
        .watchTermination(): (_, done) =>
          done.onComplete: _ =>
            var sessions = Set.empty[(UserId, ActorRef[DrivenEvent])]
            synchronized:
              sessions = activeSessions.getOrElse(groupId, Set.empty)
              activeSessions.updateWith(groupId)(_.map(_.filterNot(_._1 == userId)))
            sessions.find(_._1 == userId).map(_._2).foreach: ref =>
              sessions.foreach((uid, _) => service.removeObserverFor(Scope(uid, groupId))(Set(ref)).unsafeRunSync())
        .to(Sink.foreach(service.handle(scope)(_).unsafeRunSync()))
      val routeToClient: Source[Message, ActorRef[DrivenEvent]] =
        ActorSource.actorRef(
          completionMatcher = { case Complete => },
          failureMatcher = { case ex: Throwable => ex },
          bufferSize = 1_000,
          overflowStrategy = OverflowStrategy.fail,
        ).mapMaterializedValue: ref =>
          var sessions = Set.empty[(UserId, ActorRef[DrivenEvent])]
          synchronized:
            sessions = activeSessions.getOrElse(groupId, Set.empty)
            activeSessions.update(groupId, sessions + ((userId, ref)))
          sessions.foreach((uid, _) => service.addObserverFor(Scope(uid, groupId))(Set(ref)).unsafeRunSync())
          service.addObserverFor(scope)(sessions.map(_._2) + ref).unsafeRunSync()
          ref
        .map:
          case event: DrivenEvent => TextMessage(Json.encode(event).toUtf8String)
      Flow.fromSinkAndSource(routeToGroupActor, routeToClient)
