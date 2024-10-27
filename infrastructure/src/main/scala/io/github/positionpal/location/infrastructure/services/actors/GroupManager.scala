package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import io.github.positionpal.location.domain.UserState.Active
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent, GPSLocation, UserUpdate}
import io.github.positionpal.location.infrastructure.ws.WebSockets

/** An actor in charge of managing groups of users. */
object GroupManager:

  sealed trait ProtocolCommand extends AkkaSerializable
  case class Wire(observer: ActorRef[WebSockets.Protocol]) extends ProtocolCommand
  case class UnWire(observer: ActorRef[WebSockets.Protocol]) extends ProtocolCommand

  type Command = DrivingEvent | ProtocolCommand

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val key: EntityTypeKey[Command] = EntityTypeKey(getClass.getName)

  def apply(): Entity[Command, ShardingEnvelope[Command]] = Entity(key): entityCtx =>
    this(entityCtx.entityId)

  def apply(groupId: String, observers: Set[ActorRef[WebSockets.Protocol]] = Set.empty): Behavior[Command] =
    Behaviors.setup: ctx =>
      ctx.log.debug("Starting GroupManager::{}@{}", groupId, Cluster(ctx.system).selfMember.address)
      Behaviors.receiveMessage:
        case Wire(observer) => apply(groupId, observers + observer)
        case UnWire(observer) => apply(groupId, observers - observer)
        case e: DrivingEvent =>
          ctx.log.debug("Received event: {}", e)
          val testEvent = UserUpdate(e.timestamp, e.user, GPSLocation(0.0, 0.0), Active).asInstanceOf[DrivenEvent]
          observers.foreach(_ ! WebSockets.Reply(testEvent))
          Behaviors.same
        /* TODO
          val result = ClusterSharding(system).entityRefFor(RealTimeUserTracker.key, event.user.id) ? event
          result.onComplete:
            case Success(value) => observers.foreach(_.update(value))
            case Failure(exception) => ctx.log.error(exception.getMessage)
          Behaviors.same
         */
