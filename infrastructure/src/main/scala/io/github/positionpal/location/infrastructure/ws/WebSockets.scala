package io.github.positionpal.location.infrastructure.ws

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.ShardingEnvelope
import io.github.positionpal.location.infrastructure.services.actors.{GroupManager, WebSocketsManagers}

trait WebSockets:

  sealed trait Protocol

  object Routes:

    import akka.http.scaladsl.server.Route
    import akka.http.scaladsl.server.Directives.*
    import Handlers.*

    def groupRoute(
      manager: ActorRef[WebSocketsManagers.WebSocketsManager.Command],
      groupActor: ActorRef[ShardingEnvelope[GroupManager.Command]]
    ): Route = path("group" / Segment): groupId =>
      handleWebSocketMessages:
        handleGroupRoute(groupId, manager, groupActor)

  object Handlers:

    import akka.http.scaladsl.model.ws.{Message, TextMessage, BinaryMessage}
    import akka.NotUsed
    import akka.stream.scaladsl.{Sink, Flow, Source}
    import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
    import io.github.positionpal.location.domain.DrivenEvent

    def handleGroupRoute(
      groupId: String,
      manager: ActorRef[WebSocketsManagers.WebSocketsManager.Command],
      groupActor: ActorRef[ShardingEnvelope[GroupManager.Command]],
    ): Flow[Message, Message, ?] =
      val routeToGroupActor: Sink[Message, NotUsed] =
        Flow[Message].map:
          case message: TextMessage => ???
          case _: BinaryMessage => ???
        .to:
          ActorSink.actorRef(ref = groupActor, onCompleteMessage = ???, onFailureMessage = ???)
      val routeToClient: Source[Message, ActorRef[DrivenEvent]] =
        ActorSource.actorRef(
          completionMatcher = ???,
          failureMatcher = ???,
          bufferSize = ???,
          overflowStrategy = ???,
        ).mapMaterializedValue: actorRef =>
          ???
          actorRef
        .map:
          case drivenEvent: DrivenEvent => ???
          case _ => ???
      Flow.fromSinkAndSource(routeToGroupActor, routeToClient)
