package io.github.positionpal.location.tracking.actors

import java.time.Instant.now

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import akka.actor.typed.SupervisorStrategy.restartWithBackoff
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.*
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.RetentionCriteria.snapshotEvery
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import cats.effect.IO
import io.github.positionpal.entities.NotificationMessage
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.reactions.*
import io.github.positionpal.location.application.reactions.TrackingEventReaction.*
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.EventConversions.userUpdateFrom
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.presentation.ScopeUtils.*

/** The actor in charge of tracking the real-time location of users, reacting to
  * their movements and status changes. This actor is managed by Akka Cluster Sharding.
  */
object RealTimeUserTracker:

  /** Uniquely identifies the types of this entity instances (actors) that will be managed by cluster sharding. */
  val key: EntityTypeKey[Command] = EntityTypeKey(getClass.getSimpleName)

  /** Labels used to tag the events emitted by this kind entity actors to distribute them over several projections.
    * Each entity instance selects it (based on an appropriate strategy) and uses it to tag the events it emits.
    */
  val tags: Seq[String] = Vector.tabulate(5)(i => s"${getClass.getSimpleName}-$i")

  /** The commands that can be sent to the actor to interact with it. */
  sealed trait ProtocolCommand extends AkkaSerializable

  /** The command to attach an observer to the actor, receiving updates about the user's state. */
  case class Wire(observer: ActorRef[DrivenEvent]) extends ProtocolCommand

  /** The command to detach an observer from the actor, stopping to receive updates about the user's state. */
  case class UnWire(observer: ActorRef[DrivenEvent]) extends ProtocolCommand

  /** The responses that the actor can send to itself in respose to an asynchronous operation. */
  sealed trait SelfResponse
  case object Ignore extends SelfResponse
  case object AliveCheck extends SelfResponse

  type Observer = ActorRef[DrivenEvent]
  type Command = DrivingEvent | ProtocolCommand | SelfResponse
  type Event = StatefulDrivingEvent | ProtocolCommand

  case class StatefulDrivingEvent(state: UserState, event: DrivingEvent) extends AkkaSerializable
  object StatefulDrivingEvent:
    def from(session: Session, event: DrivingEvent): StatefulDrivingEvent =
      StatefulDrivingEvent(session.userState.next(event), event)

  case class ObservableSession(session: Session, observers: Set[Observer]) extends AkkaSerializable:
    def addObserver(observer: Observer): ObservableSession = copy(observers = observers + observer)
    def removeObserver(observer: Observer): ObservableSession = copy(observers = observers - observer)
    def update(e: DrivingEvent): ObservableSession =
      val updatedSession = session.updateWith(e)
      observers.foreach(_ ! userUpdateFrom(e, updatedSession))
      copy(session = updatedSession)
  object ObservableSession:
    def of(scope: Scope): ObservableSession = ObservableSession(Session.of(scope), Set.empty)

  def apply(
      notificationService: NotificationService[IO],
      mapsService: MapsService[IO],
  ): Entity[Command, ShardingEnvelope[Command]] =
    Entity(key)(ctx =>
      this(ctx.entityId.splitted, tags(math.abs(ctx.entityId.hashCode % tags.size)), notificationService, mapsService),
    )

  def apply(
      scope: Scope,
      projectionTag: String,
      notificationService: NotificationService[IO],
      mapsService: MapsService[IO],
  ): Behavior[Command] = Behaviors.setup: ctx =>
    given ActorContext[Command] = ctx
    Behaviors.withTimers: timer =>
      ctx.log.debug("Starting RealTimeUserTracker::{}@{}", scope, Cluster(ctx.system).selfMember.address)
      timer.startTimerAtFixedRate(AliveCheck, 10.seconds) // TODO fix
      val persistenceId = PersistenceId(key.name, scope.concatenated)
      EventSourcedBehavior(
        persistenceId,
        ObservableSession.of(scope),
        commandHandler(timer, notificationService, mapsService),
        eventHandler,
      ).withTagger(_ => Set(projectionTag))
        .snapshotWhen((_, event, _) => event == RoutingStopped, deleteEventsOnSnapshot = true)
        .withRetention(snapshotEvery(numberOfEvents = 100, keepNSnapshots = 1).withDeleteEventsOnSnapshot)
        .onPersistFailure(restartWithBackoff(minBackoff = 2.second, maxBackoff = 15.seconds, randomFactor = 0.2))

  private def eventHandler: (ObservableSession, Event) => ObservableSession = (state, event) =>
    event match
      case Wire(o) => state.addObserver(o)
      case UnWire(o) => state.removeObserver(o)
      case StatefulDrivingEvent(_, e) => state.update(e)

  private def commandHandler(
      timer: TimerScheduler[Command],
      notificationService: NotificationService[IO],
      mapsService: MapsService[IO],
  )(using ctx: ActorContext[Command]): (ObservableSession, Command) => Effect[Event, ObservableSession] =
    (state, command) =>
      command match
        case e: SampledLocation => trackingHandler(notificationService, mapsService)(state.session, e)
        case e: AliveCheck.type => aliveCheckHandler(timer)(notificationService)(state.session, e)
        case e: (RoutingStarted | RoutingStopped | SOSAlertTriggered | SOSAlertStopped) =>
          Effect.persist(StatefulDrivingEvent.from(state.session, e))
        case e: ProtocolCommand => Effect.persist(e)
        case _ => Effect.none

  import cats.effect.unsafe.implicits.global

  private def trackingHandler(
      notificationService: NotificationService[IO],
      mapsService: MapsService[IO],
  )(using ctx: ActorContext[Command]): (Session, SampledLocation) => Effect[Event, ObservableSession] =
    (session, event) =>
      session match
        case Session(_, Routing, _, Some(tracking: MonitorableTracking)) =>
          val reaction = (ArrivalCheck(mapsService) >>> StationaryCheck() >>> ArrivalTimeoutCheck())(tracking, event)
          ctx.pipeToSelf(reaction.unsafeToFuture()):
            case Success(result) => reactionHandler(session, event, result)(notificationService)
            case Failure(exception) =>
              ctx.log.error("Error while executing reaction: {}", exception.getMessage)
              Ignore
          Effect.persist(StatefulDrivingEvent.from(session, event))
        case _ => Effect.persist(StatefulDrivingEvent.from(session, event))

  private def reactionHandler(session: Session, event: DrivingEvent, result: Either[Serializable, Continue.type])(
      notificationService: NotificationService[IO],
  )(using ctx: ActorContext[Command]): Command = result match
    case Right(_) => Ignore
    case Left(Notification.Alert(msg)) =>
      notificationService.sendToGroup(session.scope.group, session.scope.user, msg).unsafeRunAndForget()
      Ignore
    case Left(Notification.Success(msg)) =>
      notificationService.sendToGroup(session.scope.group, session.scope.user, msg).unsafeRunAndForget()
      RoutingStopped(event.timestamp, event.user)
    case Left(e) =>
      ctx.log.error(e.toString)
      Ignore

  private def aliveCheckHandler(timer: TimerScheduler[Command])(
      notificationService: NotificationService[IO],
  ): (Session, AliveCheck.type) => Effect[Event, ObservableSession] = (session, _) =>
    if session.userState != Inactive && session.lastSampledLocation.get.timestamp.isBefore(now().minusSeconds(30))
    then
      timer.cancelAll()
      val event = WentOffline(session.lastSampledLocation.get.timestamp, session.lastSampledLocation.get.user)
      if session.userState == SOS || session.userState == Routing then
        val notification = NotificationMessage.create(
          s"${event.user.username()} connection lost alert!",
          s"User ${event.user.username()} went offline while in ${session.userState} mode!",
        )
        notificationService.sendToGroup(session.scope.group, session.scope.user, notification).unsafeRunAndForget()
      Effect.persist(StatefulDrivingEvent.from(session, event))
    else Effect.none
