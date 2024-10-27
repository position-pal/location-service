package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import io.github.positionpal.location.domain.{DrivenEvent, GroupId}
import io.github.positionpal.location.infrastructure.ws.WebSockets

object WebSocketsManagers:

  /** The root distributed actor in charge of managing the WebSocket connections of this
    * instance groups, acting as a dispenser of group-specific WebSocket managers.
    */
  object WebSocketsManager:

    sealed trait Command
    case class NewConnection(
        groupId: GroupId,
        ws: ActorRef[WebSockets.Protocol],
        replyTo: ActorRef[Reply],
    ) extends Command

    sealed trait Reply
    case class ManagerRef(manager: ActorRef[GroupWebsocketManager.Command]) extends Reply

    def apply(managers: Map[GroupId, ActorRef[GroupWebsocketManager.Command]] = Map.empty): Behavior[Command] =
      Behaviors.receive: (ctx, msg) =>
        msg match
          case NewConnection(gid, ws, replyTo) =>
            val manager = managers.getOrElse(gid, spawnChildManager(gid)(using ctx))
            manager ! GroupWebsocketManager.AddClient(ws)
            replyTo ! ManagerRef(manager)
            apply(managers + (gid -> manager))

    private def spawnChildManager(
        groupId: GroupId,
    )(using ctx: ActorContext[?]): ActorRef[GroupWebsocketManager.Command] =
      ctx.spawn(behavior = GroupWebsocketManager(), name = s"group-websocket-manager-${groupId.id}")

  /** The actor in charge of managing the WebSocket connections of a specific group. */
  object GroupWebsocketManager:

    sealed trait Command extends AkkaSerializable
    private[WebSocketsManagers] case class AddClient(ws: ActorRef[WebSockets.Protocol]) extends Command
    case class MessageToClient(message: DrivenEvent) extends Command

    def apply(clients: Set[ActorRef[WebSockets.Protocol]] = Set.empty): Behavior[Command] =
      Behaviors.receive: (ctx, msg) =>
        msg match
          case AddClient(ws) => apply(clients + ws)
          case MessageToClient(message) =>
            ctx.log.debug("Sending message to clients: {}", message)
            clients.foreach(_ ! WebSockets.Reply(message))
            Behaviors.same
