package io.github.positionpal.location.infrastructure.ws

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.typed.ActorRef
import akka.stream.OverflowStrategy
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent, UserId}
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.services.actors.AkkaSerializable
import io.github.positionpal.location.presentation.ModelCodecs

object WebSockets:

  sealed trait Protocol extends AkkaSerializable
  case class Reply(event: DrivenEvent) extends Protocol
  case object Complete extends Protocol
  case class Failure(ex: Throwable) extends Protocol

  object Routes:

    import Handlers.*
    import akka.http.scaladsl.server.Directives.*
    import akka.http.scaladsl.server.Route

    def groupRoute(service: ActorBasedRealTimeTracking.Service[IO, UserId]): Route =
      path("group" / Segment / Segment): (groupId, userId) =>
        handleWebSocketMessages:
          handleGroupRoute(groupId, userId, service)

  object Handlers extends ModelCodecs:

    import akka.http.scaladsl.model.ws.{Message, TextMessage}
    import akka.stream.scaladsl.{Flow, Sink, Source}
    import akka.stream.typed.scaladsl.ActorSource
    import io.bullet.borer.Json

    private val activeSessions = scala.collection.mutable.Map[String, Set[(String, ActorRef[Protocol])]]()

    def handleGroupRoute(
        groupId: String,
        userId: String,
        service: ActorBasedRealTimeTracking.Service[IO, UserId],
    ): Flow[Message, Message, ?] =
      val routeToGroupActor: Sink[Message, Unit] =
        Flow[Message].map:
          case TextMessage.Strict(msg) => Json.decode(msg.getBytes).to[DrivingEvent].valueEither
          case _ => Left("Invalid message")
        .collect { case Right(e) => e }.watchTermination(): (_, done) =>
          done.onComplete: _ =>
            activeSessions.getOrElse(groupId, Set.empty).foreach: (uid, ref) =>
              service.removeObserverFor(UserId(uid))(Set(ref)).unsafeRunSync()
            activeSessions.updateWith(groupId)(_.map(_.filterNot(_._1 == userId)))
            println(s"Active sessions: $activeSessions")
        .to(Sink.foreach(e => service.handle(e).unsafeRunSync()))
      val routeToClient: Source[Message, ActorRef[Protocol]] =
        ActorSource.actorRef(
          completionMatcher = { case Complete => },
          failureMatcher = { case Failure(ex) => ex },
          bufferSize = 1_000,
          overflowStrategy = OverflowStrategy.fail,
        ).mapMaterializedValue: ref =>
          activeSessions.getOrElse(groupId, Set.empty).foreach: (uid, _) =>
            service.addObserverFor(UserId(uid))(Set(ref)).unsafeRunSync()
          activeSessions.updateWith(groupId)(existing => Some(existing.getOrElse(Set.empty) + ((userId, ref))))
          println(s"Active sessions: $activeSessions")
          service.addObserverFor(UserId(userId))(activeSessions(groupId).map(_._2)).unsafeRunSync()
          ref
        .map(msg => TextMessage.Strict(msg.toString))
      Flow.fromSinkAndSource(routeToGroupActor, routeToClient)
