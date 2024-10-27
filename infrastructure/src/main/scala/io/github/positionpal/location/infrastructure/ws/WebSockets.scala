package io.github.positionpal.location.infrastructure.ws

import akka.actor.typed.ActorRef
import akka.stream.OverflowStrategy
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent, GroupId}
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

    def groupRoute(service: ActorBasedRealTimeTracking.Service[IO]): Route =
      path("group" / Segment): groupId =>
        handleWebSocketMessages:
          handleGroupRoute(groupId, service)

  object Handlers extends ModelCodecs:

    import akka.NotUsed
    import akka.http.scaladsl.model.ws.{Message, TextMessage}
    import akka.stream.scaladsl.{Flow, Sink, Source}
    import akka.stream.typed.scaladsl.ActorSource
    import io.bullet.borer.Json
    import io.github.positionpal.location.domain.DrivenEvent

    def handleGroupRoute(groupId: String, service: ActorBasedRealTimeTracking.Service[IO]): Flow[Message, Message, ?] =
      val routeToGroupActor: Sink[Message, NotUsed] =
        Flow[Message].map:
          case TextMessage.Strict(msg) => Json.decode(msg.getBytes).to[DrivingEvent].valueEither
          case _ => Left("Invalid message")
        .collect { case Right(e) => e }.to(Sink.foreach(e => service.handleFor(GroupId(groupId))(e).unsafeToFuture()))
      val routeToClient: Source[Message, ActorRef[DrivenEvent]] =
        ActorSource.actorRef(
          completionMatcher = { case Complete => },
          failureMatcher = { case Failure(ex) => ex },
          bufferSize = 1_000,
          overflowStrategy = OverflowStrategy.fail,
        ).mapMaterializedValue: actorRef =>
          service.addObserverFor(GroupId(groupId))(actorRef).unsafeToFuture()
          actorRef
        .map:
          case msg => TextMessage.Strict(msg.toString)
      Flow.fromSinkAndSource(routeToGroupActor, routeToClient)
