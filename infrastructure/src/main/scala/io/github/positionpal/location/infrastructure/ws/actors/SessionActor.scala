package io.github.positionpal.location.infrastructure.ws.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.typed.Cluster
import io.github.positionpal.location.infrastructure.ws.Protocol.{NewConnection, OutgoingEvent, WsMsg}

object SessionActor:

  type WebSockets = Map[String, Set[ActorRef[OutgoingEvent]]]

  def apply(wss: WebSockets = Map.empty): Behavior[OutgoingEvent] = Behaviors.receive: (ctx, msg) =>
    msg match
      case WsMsg(gid, content) =>
        ctx.log.debug("[SessionActor @ {}] Sending message to group {}", Cluster(ctx.system).selfMember.address, gid)
        wss(gid).foreach(_ ! WsMsg(gid, content))
        Behaviors.same
      case NewConnection(uid, ws) =>
        ctx.log.debug("[SessionActor @ {}] New connection for user {}", Cluster(ctx.system).selfMember.address, uid)
        apply(wss + (uid -> (wss.getOrElse(uid, Set.empty) + ws)))
      case _ => Behaviors.same
