package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import io.github.positionpal.location.domain.{DrivenEvent, GroupId}
import io.github.positionpal.location.infrastructure.services.Protocol

object WebSocketsManagers:

  /** The root distributed actor in charge of managing the WebSocket connections of this
    * instance groups, acting as a dispenser of group-specific WebSocket managers.
    */
  object WebSocketsManager:

    sealed trait Command extends AkkaSerializable
    case class NewConnection(
        groupId: GroupId,
        ws: ActorRef[Protocol.WebSocketEvent],
        replyTo: ActorRef[Reply],
    ) extends Command

    sealed trait Reply extends AkkaSerializable
    case class ManagerRef(manager: ActorRef[GroupWebsocketManager.Command]) extends Reply

    def apply(
        groupSocketManagers: Map[GroupId, ActorRef[GroupWebsocketManager.Command]] = Map.empty,
    ): Behavior[Command] =
      Behaviors.receive: (ctx, msg) =>
        msg match
          case NewConnection(gid, ws, replyTo) =>
            val manager = groupSocketManagers.getOrElse(gid, spawnChildManager(gid)(using ctx))
            manager ! GroupWebsocketManager.AddClient(ws)
            replyTo ! ManagerRef(manager)
            apply(groupSocketManagers + (gid -> manager))

    private def spawnChildManager(groupId: GroupId)(using
        ctx: ActorContext[?],
    ): ActorRef[GroupWebsocketManager.Command] =
      ctx.spawn(behavior = GroupWebsocketManager(), name = s"group-websocket-manager-$groupId")

  /** The actor in charge of managing the WebSocket connections of a specific group. */
  object GroupWebsocketManager:

    sealed trait Command extends AkkaSerializable
    private[WebSocketsManagers] case class AddClient(ws: ActorRef[Protocol.WebSocketEvent]) extends Command
    case class MessageToClient(message: DrivenEvent) extends Command

    def apply(clients: Set[ActorRef[Protocol.WebSocketEvent]] = Set.empty): Behavior[Command] =
      Behaviors.receive: (ctx, msg) =>
        msg match
          case AddClient(ws) => apply(clients + ws)
          case MessageToClient(message) =>
            ctx.log.debug("Sending message to clients: {}", message)
            clients.foreach(_ ! Protocol.MessageToClient(message))
            Behaviors.same
