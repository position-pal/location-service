package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import io.github.positionpal.location.domain.DrivingEvent
import io.github.positionpal.location.infrastructure.services.actors.WebSocketsManagers.GroupWebsocketManager

/** An actor in charge of managing groups of users. */
object GroupManager:

  trait ProtocolCommand extends AkkaSerializable
  case class Wire(observer: ActorRef[GroupWebsocketManager.Command]) extends ProtocolCommand
  case class UnWire(observer: ActorRef[GroupWebsocketManager.Command]) extends ProtocolCommand

  type Command = DrivingEvent | ProtocolCommand

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val key: EntityTypeKey[Command] = EntityTypeKey(getClass.getName)

  def apply(): Entity[Command, ShardingEnvelope[Command]] = Entity(key): entityCtx =>
    this(entityCtx.entityId)

  def apply(groupId: String): Behavior[Command] = Behaviors.setup: ctx =>
    ctx.log.debug("Starting GroupManager::{}@{}", groupId, Cluster(ctx.system).selfMember.address)
    /*
    var observers = Set.empty[ActorObserver[IO]] // TODO: to be placed in the constructor
    command match
      case Wire(observer) => apply(groupId, observers + observer)
      case UnWire(observer) => apply(groupId, observers - observer)
      case ev: DrivingEvent =>
        val result = ClusterSharding(system).entityRefFor(RealTimeUserTracker.key, event.user.id) ? event
        result.onComplete:
          case Success(value) => observers.foreach(_.update(value))
          case Failure(exception) => ctx.log.error(exception.getMessage)
        Behaviors.same
     */
    Behaviors.same
