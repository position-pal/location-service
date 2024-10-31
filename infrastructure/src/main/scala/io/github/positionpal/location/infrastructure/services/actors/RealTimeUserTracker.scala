package io.github.positionpal.location.infrastructure.services.actors

import java.time.Instant
import java.util.Date

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.*
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.github.positionpal.location.application.reactions.*
import io.github.positionpal.location.application.reactions.TrackingEventReaction.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.EventConversions.{*, given}
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.infrastructure.geo.*
import io.github.positionpal.location.infrastructure.ws.WebSockets

/** The actor in charge of tracking the real-time location of users, reacting to
  * their movements and status changes. This actor is managed by Akka Cluster Sharding.
  */
object RealTimeUserTracker:

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val key: EntityTypeKey[Command] = EntityTypeKey(getClass.getName)

  sealed trait ProtocolCommand extends AkkaSerializable
  case class Wire(observer: ActorRef[WebSockets.Protocol]) extends ProtocolCommand
  case class UnWire(observer: ActorRef[WebSockets.Protocol]) extends ProtocolCommand

  case object Ignore
  case object AliveCheck

  type Command = DrivingEvent | Ignore.type | AliveCheck.type | ProtocolCommand
  type Event = DrivingEvent | ProtocolCommand
  private type T = Tracking | MonitorableTracking

  final case class State(
      userState: UserState,
      tracking: Option[T],
      lastSample: Option[SampledLocation],
      observers: Set[ActorRef[WebSockets.Protocol]],
  ) extends AkkaSerializable:
    def update(e: DrivingEvent): State =
      val newState = e match
        case ev: SampledLocation =>
          userState match
            case Routing | SOS => copy(tracking = tracking.map(_ + ev), lastSample = Some(ev))
            case _ => copy(userState = Active, tracking = tracking.map(_ + ev), lastSample = Some(ev))
        case ev: RoutingStarted => copy(userState = Routing, tracking = Some(ev.toMonitorableTracking))
        case ev: SOSAlertTriggered => copy(userState = SOS, tracking = Some(ev.toTracking), lastSample = Some(ev))
        case _: (SOSAlertStopped | RoutingStopped) => copy(userState = Active, tracking = None)
        case _: WentOffline => copy(userState = Inactive)
      observers.foreach:
        _ ! WebSockets.Reply(UserUpdate(e.timestamp, e.user, newState.lastSample.map(_.position), newState.userState))
      newState

  object State:
    def empty: State = State(UserState.Inactive, None, None, Set.empty)

  def apply(): Entity[Command, ShardingEnvelope[Command]] = Entity(key): entityCtx =>
    this(entityCtx.entityId)

  def apply(entityId: String): Behavior[Command] = Behaviors.setup: ctx =>
    given ActorContext[Command] = ctx
    Behaviors.withTimers: timer =>
      ctx.log.debug("Starting RealTimeUserTracker::{}@{}", entityId, Cluster(ctx.system).selfMember.address)
      timer.startTimerAtFixedRate(AliveCheck, 10.seconds)
      EventSourcedBehavior(PersistenceId(key.name, entityId), State.empty, commandHandler(timer), eventHandler)

  private def eventHandler: (State, Event) => State = (state, event) =>
    event match
      case Wire(observer) => State(state.userState, state.tracking, state.lastSample, state.observers + observer)
      case UnWire(observer) => State(state.userState, state.tracking, state.lastSample, state.observers - observer)
      case ev: DrivingEvent => state.update(ev)

  private def commandHandler(
      timer: TimerScheduler[Command],
  )(using ActorContext[Command]): (State, Command) => Effect[Event, State] = (state, command) =>
    command match
      case ev: SampledLocation => trackingHandler(state, ev)
      case ev: (RoutingStarted | RoutingStopped | SOSAlertTriggered | SOSAlertStopped) => Effect.persist(ev)
      case ev: AliveCheck.type => aliveCheckHandler(timer)(state, ev)
      case ev: ProtocolCommand => Effect.persist(ev)
      case _ => Effect.none

  private def trackingHandler(using ctx: ActorContext[Command]): (State, SampledLocation) => Effect[Event, State] =
    import cats.effect.unsafe.implicits.global
    (state, event) =>
      state match
        case State(Routing, Some(tracking: MonitorableTracking), _, _) =>
          ctx.pipeToSelf(reaction(tracking, event).unsafeToFuture()):
            case Success(result) => reactionHandler(event)(result)
            case Failure(exception) =>
              ctx.log.error(exception.getMessage)
              Ignore
          Effect.persist(event)
        case _ => Effect.persist(event)

  private def reaction(tracking: MonitorableTracking, event: SampledLocation) =
    for
      config <- MapboxConfigurationProvider("MAPBOX_API_KEY").configuration
      checks = ArrivalCheck(MapboxService()) >>> StationaryCheck() >>> ArrivalTimeoutCheck()
      result <- checks(tracking, event).value.run(config)
    yield result.flatten

  private def reactionHandler(event: DrivingEvent)(
      result: Either[Serializable, Continue.type],
  )(using ctx: ActorContext[Command]): Command = result match
    case Right(_) => Ignore
    case Left(Notification.Alert(msg)) =>
      ctx.log.debug(msg)
      Ignore
    case Left(Notification.Success(msg)) =>
      ctx.log.debug(msg)
      RoutingStopped(event.timestamp, event.user)
    case Left(e) =>
      ctx.log.error(e.toString)
      Ignore

  private def aliveCheckHandler(
      timer: TimerScheduler[Command],
  )(using ctx: ActorContext[Command]): (State, AliveCheck.type) => Effect[Event, State] =
    (state, _) =>
      if state.lastSample.isDefined && state.lastSample.get.timestamp.before(Date.from(Instant.now().minusSeconds(30)))
      then
        timer.cancelAll()
        if state.userState == SOS || state.userState == Routing then
          ctx.log.info("User {} went offline", state.lastSample.get.user)
        Effect.persist(WentOffline(state.lastSample.get.timestamp, state.lastSample.get.user))
      else Effect.none
