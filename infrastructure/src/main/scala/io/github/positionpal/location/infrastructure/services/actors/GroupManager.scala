package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
//import akka.util.Timeout
import io.github.positionpal.location.domain.DrivingEvent
import io.github.positionpal.location.infrastructure.ws.WebSockets

//import scala.concurrent.ExecutionContext
//import scala.concurrent.duration.DurationInt

/** An actor in charge of managing groups of users. */
object GroupManager:

  sealed trait ProtocolCommand extends AkkaSerializable
  case class Wire(observer: ActorRef[WebSockets.Protocol]) extends ProtocolCommand
  case class UnWire(observer: ActorRef[WebSockets.Protocol]) extends ProtocolCommand

  type Command = DrivingEvent | ProtocolCommand

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val key: EntityTypeKey[Command] = EntityTypeKey(getClass.getName)

  def apply()(using actorSystem: ActorSystem[?]): Entity[Command, ShardingEnvelope[Command]] = Entity(key): entityCtx =>
    this(entityCtx.entityId)

//  private given askTimeout: Timeout = Timeout(5.seconds)

  def apply(
    groupId: String,
    observers: Set[ActorRef[WebSockets.Protocol]] = Set.empty
  )(using actorSystem: ActorSystem[?]): Behavior[Command] =
//    given ExecutionContext = actorSystem.executionContext
    Behaviors.setup: ctx =>
      ctx.log.debug("Starting GroupManager::{}@{}", groupId, Cluster(ctx.system).selfMember.address)
      Behaviors.receiveMessage:
        case Wire(observer) => apply(groupId, observers + observer)
        case UnWire(observer) => apply(groupId, observers - observer)
        case e: DrivingEvent =>
          ctx.log.debug("Received event: {}", e)
//          val res = ClusterSharding(actorSystem).entityRefFor(RealTimeUserTracker.key, e.user.id) ? RealTimeUserTracker.React(e)
//          res.onComplete:
//            case scala.util.Success(Event(value)) =>
//              println(s">>>> Comeback from RealTimeUserTracker: $value")
//              observers.foreach(_ ! WebSockets.Reply(value))
//            case msg => println(s">>> $msg")
          Behaviors.same
//        case _ => Behaviors.same
