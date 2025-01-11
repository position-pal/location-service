package io.github.positionpal.location.tracking.actors

import scala.concurrent.duration.DurationInt

import akka.persistence.typed.PersistenceId
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.actor.typed.SupervisorStrategy.restartWithBackoff
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.scaladsl.RetentionCriteria.snapshotEvery
import io.github.positionpal.location.commons.ScopeFunctions.also
import io.github.positionpal.location.domain.DrivenEvent
import io.github.positionpal.entities.GroupId

/** An akka sharded actor that manages a group of users, acting as an intermediary
  * between the clients and the [[RealTimeUserTracker]] actors that track the users.
  */
object GroupManager:

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val key: EntityTypeKey[Command] = EntityTypeKey(getClass.getSimpleName)

  type Observer = ActorRef[DrivenEvent]
  type Command = ProtocolCommand | DrivenEvent
  type Event = ProtocolCommand

  /** The commands that can be sent to the actor to interact with it. */
  sealed trait ProtocolCommand extends AkkaSerializable

  /** The command to attach an observer to the actor, receiving updates about the user's state. */
  case class Wire(observer: Observer) extends ProtocolCommand

  /** The command to detach an observer from the actor, stopping to receive updates about the user's state. */
  case class UnWire(observer: Observer) extends ProtocolCommand

  final case class State(observers: Set[Observer] = Set.empty) extends AkkaSerializable:
    def update(event: DrivenEvent): State = this.also(_.observers.foreach(_ ! event))

  def apply(): Entity[Command, ShardingEnvelope[Command]] =
    Entity(key)(ctx => this(GroupId.create(ctx.entityId)))

  def apply(groupId: GroupId): Behavior[Command] =
    Behaviors.setup: ctx =>
      given ActorContext[Command] = ctx
      val persistenceId = PersistenceId(key.name, groupId.value())
      EventSourcedBehavior(persistenceId, State(), commandHandler, eventHandler)
        .withRetention(snapshotEvery(numberOfEvents = 100, keepNSnapshots = 1).withDeleteEventsOnSnapshot)
        .onPersistFailure(restartWithBackoff(minBackoff = 2.second, maxBackoff = 15.seconds, randomFactor = 0.2))

  private def commandHandler(using ctx: ActorContext[Command]): (State, Command) => Effect[Event, State] =
    (_, command) =>
      ctx.log.debug("Received command: {}", command)
      command match
        case e: ProtocolCommand => Effect.persist(e)
        case e: DrivenEvent => Effect.none.thenRun(_.update(e))

  private def eventHandler(using ctx: ActorContext[Command]): (State, Event) => State =
    (state, event) =>
      ctx.log.debug("Received event: {}", event)
      event match
        case Wire(observer) => state.copy(observers = state.observers + observer)
        case UnWire(observer) => state.copy(observers = state.observers - observer)
