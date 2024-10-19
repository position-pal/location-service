package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import akka.cluster.typed.Cluster

object GroupActor:
  val key: EntityTypeKey[String] = EntityTypeKey[String]("GroupActor")

  def apply(): Entity[String, ShardingEnvelope[String]] = Entity(key): entityCtx =>
    this(entityCtx.entityId)

  def apply(entityId: String): Behavior[String] =
    Behaviors.setup: ctx =>
      ctx.log.debug("Starting RealTimeUserTracker::{}@{}", entityId, Cluster(ctx.system).selfMember.address)
      Behaviors.receiveMessage:
        message =>
          ctx.log.debug("Received message: {}", message)
          Behaviors.same
