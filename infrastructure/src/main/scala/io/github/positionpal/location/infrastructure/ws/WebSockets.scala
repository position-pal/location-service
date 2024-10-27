package io.github.positionpal.location.infrastructure.ws

import scala.concurrent.ExecutionContext

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.util.Timeout
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent, GroupId}
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.services.actors.WebSocketsManagers
import io.github.positionpal.location.presentation.ModelCodecs

object WebSockets:

  sealed trait Protocol
  case class Reply(event: DrivenEvent) extends Protocol
  case object Complete extends Protocol
  case class Failure(ex: Throwable) extends Protocol

  object Routes:

    import Handlers.*
    import akka.http.scaladsl.server.Directives.*
    import akka.http.scaladsl.server.Route

    def groupRoute(
        service: ActorBasedRealTimeTracking.Service[IO],
        manager: ActorRef[WebSocketsManagers.WebSocketsManager.Command],
    )(using actorSystem: ActorSystem[?]): Route = path("group" / Segment): groupId =>
      handleWebSocketMessages:
        handleGroupRoute(groupId, service, manager)

  object Handlers extends ModelCodecs:

    import akka.NotUsed
    import akka.http.scaladsl.model.ws.{Message, TextMessage}
    import akka.stream.scaladsl.{Flow, Sink, Source}
    import akka.stream.typed.scaladsl.ActorSource
    import io.bullet.borer.Json
    import akka.actor.typed.scaladsl.AskPattern._
    import io.github.positionpal.location.domain.DrivenEvent
    import scala.concurrent.duration.DurationInt

    def handleGroupRoute(
        groupId: String,
        service: ActorBasedRealTimeTracking.Service[IO],
        manager: ActorRef[WebSocketsManagers.WebSocketsManager.Command],
    )(using actorSystem: ActorSystem[?]): Flow[Message, Message, ?] =
      given Timeout = 3.seconds
      given ExecutionContext = actorSystem.executionContext
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
          for
            reply <- manager.ask(WebSocketsManagers.WebSocketsManager.NewConnection(GroupId(groupId), actorRef, _))
            managerRef = reply match
              case WebSocketsManagers.WebSocketsManager.ManagerRef(ref) => ref
            _ <- service.addObserverFor(GroupId(groupId))(managerRef).unsafeToFuture()
          yield ()
          actorRef
        .map:
          case drivenEvent: DrivenEvent => TextMessage.Strict(Json.encode(drivenEvent).toUtf8String)
          case _ => TextMessage.Strict("Invalid message")
      Flow.fromSinkAndSource(routeToGroupActor, routeToClient)
