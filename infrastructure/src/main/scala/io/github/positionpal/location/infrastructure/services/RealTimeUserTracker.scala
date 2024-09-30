package io.github.positionpal.location.infrastructure.services

import scala.util.{Failure, Success}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.github.positionpal.location.application.reactions.*
import io.github.positionpal.location.application.reactions.TrackingEventReaction.Notification
import io.github.positionpal.location.application.services.UserState
import io.github.positionpal.location.application.services.UserState.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.infrastructure.geo.*

object RealTimeUserTracker:

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("RealTimeUserTracker")

  case object Ignore

  type Command = DrivingEvent | Ignore.type
  type Event = DrivingEvent

  final case class State(userState: UserState, route: Option[Route], lastSample: Option[Tracking]) extends Serializable
  object State:
    def empty: State = State(UserState.Inactive, None, None)

  import cats.effect.unsafe.implicits.global

  /** Configure this actor to be managed by cluster sharding.
    * @return the [[Entity]] instance that will be managed by cluster sharding.
    */
  def apply(): Entity[Command, ShardingEnvelope[Command]] =
    Entity(typeKey): entityCtx =>
      apply(entityCtx.entityId)

  def apply(entityId: String): Behavior[Command] =
    Behaviors.setup: ctx =>
      ctx.log.debug("Starting RealTimeUserTracker::{}@{}", entityId, Cluster(ctx.system).selfMember.address)
      EventSourcedBehavior(PersistenceId(typeKey.name, entityId), State.empty, commandHandler(ctx), eventHandler)

  private val eventHandler: (State, Event) => State = (state, event) =>
    event match
      case ev: StartRouting => State(Routing, Some(Route(ev)), state.lastSample)
      case ev: SOSAlert => ??? // State(SOS, Some(Route(ev)), state.lastSample)
      case ev: Tracking => State(Active, None, Some(ev))
      case ev: StopSOS => State(Active, None, state.lastSample)
      case ev: StopRouting => State(Active, None, state.lastSample)

  private def commandHandler(ctx: ActorContext[Command]): (State, Command) => Effect[Event, State] =
    (state, command) =>
      command match
        case ev: Tracking => trackingHandler(ctx)(state, ev)
        case ev: StartRouting => routingHandler(state, ev)
        case ev: StopRouting => routingHandler(state, ev)
        case _ => Effect.none

  private val routingHandler: (State, StartRouting | StopRouting) => Effect[DrivingEvent, State] =
    (_, event) => Effect.persist(event)

  private def trackingHandler(ctx: ActorContext[Command]): (State, Tracking) => Effect[DrivingEvent, State] =
    (state, event) =>
      state match
        case State(Routing, Some(route), _) =>
          ctx.pipeToSelf(reaction(route, event).unsafeToFuture()):
            case Success(value) =>
              value match
                case Right(_) =>
                  ctx.log.debug("Routing continuing...")
                  Ignore
                case Left(Notification.Alert(msg)) =>
                  ctx.log.debug(msg)
                  Ignore
                case Left(Notification.Success(msg)) =>
                  ctx.log.debug(msg)
                  StopRouting(event.timestamp, event.user)
                case Left(mapError: String) =>
                  ctx.log.error(mapError)
                  Ignore
            case Failure(exception) =>
              ctx.log.error(exception.getMessage)
              Ignore
          ctx.log.debug("Routing...")
          Effect.persist(event)
        case _ => Effect.persist(event)

  private def reaction(route: Route, event: Tracking) =
    for
      config <- MapboxConfigurationProvider("MAPBOX_API_KEY").configuration
      checks = ArrivalCheck(MapboxService()) >>> StationaryCheck() >>> ArrivalTimeoutCheck()
      result <- checks(route, event).value.run(config)
    yield result.flatten
