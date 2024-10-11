package io.github.positionpal.location.infrastructure.services

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.*
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.github.positionpal.location.application.reactions.*
import io.github.positionpal.location.application.reactions.TrackingEventReaction.Notification
import io.github.positionpal.location.application.services.UserState
import io.github.positionpal.location.application.services.UserState.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.EventConversions.{*, given}
import io.github.positionpal.location.infrastructure.geo.*

object RealTimeUserTracker:

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](getClass.getName)

  case object Ignore
  case object AliveCheck

  type Command = DomainEvent | Ignore.type | AliveCheck.type
  type Event = DomainEvent
  private type T = Tracking | MonitorableTracking

  final case class State(
      userState: UserState,
      tracking: Option[T], // TODO: keep in memory a subset of the tracking data!
      lastSample: Option[SampledLocation],
  ) extends Serializable
  object State:
    def empty: State = State(UserState.Inactive, None, None)

  /** Configure this actor to be managed by cluster sharding.
    * @return the [[Entity]] instance that will be managed by cluster sharding.
    */
  def apply(): Entity[Command, ShardingEnvelope[Command]] =
    Entity(typeKey): entityCtx =>
      apply(entityCtx.entityId)

  def apply(entityId: String): Behavior[Command] =
    Behaviors.setup: ctx =>
      Behaviors.withTimers: timer =>
        ctx.log.debug("Starting RealTimeUserTracker::{}@{}", entityId, Cluster(ctx.system).selfMember.address)
        timer.startTimerAtFixedRate(AliveCheck, 20.seconds)
        EventSourcedBehavior(
          PersistenceId(typeKey.name, entityId),
          State.empty,
          commandHandler(ctx, timer),
          eventHandler,
        )

  private val eventHandler: (State, Event) => State = (state, event) =>
    event match
      case ev: RoutingStarted => State(Routing, Some(ev.toMonitorableTracking), state.lastSample)
      case ev: SOSAlertTriggered => State(SOS, Some(Tracking(ev.user)), Some(ev))
      case ev: SampledLocation =>
        state match
          case State(Routing | SOS, Some(tracking), _) => State(Routing, Some(tracking + ev), Some(ev))
          case _ => State(Active, None, Some(ev))
      case _: (SOSAlertStopped | RoutingStopped) => State(Active, None, state.lastSample)
      case _: WentOffline => State(Inactive, state.tracking, state.lastSample)

  private def commandHandler(
      ctx: ActorContext[Command],
      timer: TimerScheduler[Command],
  ): (State, Command) => Effect[Event, State] = (state, command) =>
    command match
      case ev: SampledLocation => trackingHandler(ctx)(state, ev)
      case ev: (RoutingStarted | RoutingStopped | SOSAlertTriggered | SOSAlertStopped) => Effect.persist(ev)
      case ev: AliveCheck.type => aliveCheckHandler(timer)(state, ev)
      case _ => Effect.none

  private def trackingHandler(ctx: ActorContext[Command]): (State, SampledLocation) => Effect[Event, State] =
    import cats.effect.unsafe.implicits.global
    (state, event) =>
      state match
        case State(Routing, Some(tracking: MonitorableTracking), _) =>
          ctx.pipeToSelf(reaction(tracking, event).unsafeToFuture()):
            case Success(result) => reactionHandler(ctx)(event)(result)
            case Failure(exception) =>
              ctx.log.error(exception.getMessage)
              Ignore
          ctx.log.debug("Routing...")
          Effect.persist(event)
        case _ => Effect.persist(event)

  private def reaction(tracking: MonitorableTracking, event: SampledLocation) =
    for
      config <- MapboxConfigurationProvider("MAPBOX_API_KEY").configuration
      checks = ArrivalCheck(MapboxService()) >>> StationaryCheck() >>> ArrivalTimeoutCheck()
      result <- checks(tracking, event).value.run(config)
    yield result.flatten

  private def reactionHandler(ctx: ActorContext[Command])(event: Event)(
      result: Either[java.io.Serializable, TrackingEventReaction.Continue.type],
  ): Command = result match
    case Right(_) =>
      ctx.log.debug("Routing continuing...")
      Ignore
    case Left(Notification.Alert(msg)) =>
      ctx.log.debug(msg)
      Ignore
    case Left(Notification.Success(msg)) =>
      ctx.log.debug(msg)
      RoutingStopped(event.timestamp, event.user)
    case Left(e) =>
      ctx.log.error(e.toString)
      Ignore

  private def aliveCheckHandler(timer: TimerScheduler[Command]): (State, AliveCheck.type) => Effect[Event, State] =
    (state, _) =>
      if state.lastSample.get.timestamp.before(java.util.Date.from(java.time.Instant.now().minusSeconds(30))) then
        timer.cancelAll()
        if state.userState == SOS || state.userState == Routing then ??? // send alert notification
        Effect.persist(WentOffline(state.lastSample.get.timestamp, state.lastSample.get.user))
      else Effect.none
