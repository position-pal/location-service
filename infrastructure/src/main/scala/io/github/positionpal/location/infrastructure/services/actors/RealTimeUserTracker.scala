package io.github.positionpal.location.infrastructure.services.actors

import java.time.Instant

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
import io.github.positionpal.location.domain.EventConversions.userUpdateFrom
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
  type Observer = ActorRef[WebSockets.Protocol]

  case class ObservableSession(session: Session, observers: Set[Observer]) extends AkkaSerializable:
    def addObserver(observer: Observer): ObservableSession = copy(observers = observers + observer)
    def removeObserver(observer: Observer): ObservableSession = copy(observers = observers - observer)
    def update(e: DrivingEvent): ObservableSession =
      val updatedSession = session.updateWith(e)
      observers.foreach(_ ! WebSockets.Reply(userUpdateFrom(e, updatedSession)))
      copy(session = updatedSession)

  object ObservableSession:
    def of(userId: String): ObservableSession = ObservableSession(Session.of(UserId(userId)), Set.empty)

  def apply(): Entity[Command, ShardingEnvelope[Command]] = Entity(key)(ctx => this(ctx.entityId))

  def apply(userId: String): Behavior[Command] = Behaviors.setup: ctx =>
    given ActorContext[Command] = ctx
    Behaviors.withTimers: timer =>
      ctx.log.debug("Starting RealTimeUserTracker::{}@{}", userId, Cluster(ctx.system).selfMember.address)
      timer.startTimerAtFixedRate(AliveCheck, 10.seconds)
      EventSourcedBehavior(
        PersistenceId(key.name, userId),
        ObservableSession.of(userId),
        commandHandler(timer),
        eventHandler,
      )

  private def eventHandler: (ObservableSession, Event) => ObservableSession = (state, event) =>
    event match
      case Wire(o) => state.addObserver(o)
      case UnWire(o) => state.removeObserver(o)
      case ev: DrivingEvent => state.update(ev)

  private def commandHandler(
      timer: TimerScheduler[Command],
  )(using ActorContext[Command]): (ObservableSession, Command) => Effect[Event, ObservableSession] = (state, command) =>
    command match
      case e: SampledLocation => trackingHandler(state.session, e)
      case e: AliveCheck.type => aliveCheckHandler(timer)(state.session, e)
      case e: (RoutingStarted | RoutingStopped | SOSAlertTriggered | SOSAlertStopped | ProtocolCommand) =>
        Effect.persist(e)
      case _ => Effect.none

  private def trackingHandler(using
      ctx: ActorContext[Command],
  ): (Session, SampledLocation) => Effect[Event, ObservableSession] =
    import cats.effect.unsafe.implicits.global
    (session, event) =>
      session match
        case Session(_, Routing, _, Some(tracking: MonitorableTracking)) =>
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
  )(using ctx: ActorContext[Command]): (Session, AliveCheck.type) => Effect[Event, ObservableSession] =
    (session, _) =>
      if session.userState != Inactive && session.lastSampledLocation.get.timestamp
          .isBefore(Instant.now().minusSeconds(30))
      then
        timer.cancelAll()
        if session.userState == SOS || session.userState == Routing then
          ctx.log.info("User {} went offline", session.lastSampledLocation.get.user)
        Effect.persist(WentOffline(session.lastSampledLocation.get.timestamp, session.lastSampledLocation.get.user))
      else Effect.none
