package io.github.positionpal.location.infrastructure.ws.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import akka.cluster.typed.Cluster
import io.github.positionpal.location.infrastructure.ws.Protocol
import io.github.positionpal.location.infrastructure.ws.Protocol.{Connected, OutgoingEvent, WsMsg}

object GroupActor:
  val key: EntityTypeKey[Protocol.IncomingEvent] = EntityTypeKey[Protocol.IncomingEvent]("GroupActor")

  def apply(): Entity[Protocol.IncomingEvent, ShardingEnvelope[Protocol.IncomingEvent]] =
    Entity(key): entityCtx =>
      this(entityCtx.entityId)

  def apply(groupId: String, ws: Option[ActorRef[OutgoingEvent]] = None): Behavior[Protocol.IncomingEvent] =
    Behaviors.setup: ctx =>
      ctx.log.debug("Starting GroupActor::{}@{}", groupId, Cluster(ctx.system).selfMember.address)
      Behaviors.receiveMessage:
        case Connected(ws) =>
          ctx.log.debug("[GroupActor::{}@{}] Add ws handler", groupId, Cluster(ctx.system).selfMember.address)
          apply(groupId, Some(ws))
        case msg =>
          ctx.log.debug("[SessionActor::{}@{}] Received msg {}", groupId, Cluster(ctx.system).selfMember.address, msg)
          ws.foreach(_ ! WsMsg(groupId, s"<<< $msg"))
          Behaviors.same
