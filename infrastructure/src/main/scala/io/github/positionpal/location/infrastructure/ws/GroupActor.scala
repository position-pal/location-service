package io.github.positionpal.location.infrastructure.ws

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import akka.cluster.typed.Cluster
import io.github.positionpal.location.infrastructure.ws.Protocol.{Connected, OutgoingEvent, WsMsg}

object GroupActor:
  val key: EntityTypeKey[Protocol.IncomingEvent] = EntityTypeKey[Protocol.IncomingEvent]("GroupActor")

  def apply(): Entity[Protocol.IncomingEvent, ShardingEnvelope[Protocol.IncomingEvent]] =
    Entity(key): entityCtx =>
      println(s"[Group] Creating a new entity group for ${entityCtx.entityId}")
      this(entityCtx.entityId)

  def apply(entityId: String, wss: Set[ActorRef[OutgoingEvent]] = Set.empty): Behavior[Protocol.IncomingEvent] =
    Behaviors.setup: ctx =>
      ctx.log.debug("Starting GroupActor::{}@{}", entityId, Cluster(ctx.system).selfMember.address)
      Behaviors.receiveMessage:
        case Connected(ws) =>
          apply(entityId, wss + ws)
        case msg =>
          ctx.log.info(s"Received $msg")
          wss.foreach(_ ! WsMsg(s"<<< $msg"))
          Behaviors.same
