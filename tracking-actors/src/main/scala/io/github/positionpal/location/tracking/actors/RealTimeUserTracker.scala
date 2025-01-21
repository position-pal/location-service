package io.github.positionpal.location.tracking.actors

import java.time.Instant.now

import scala.util.{Failure, Success}
import scala.concurrent.duration.DurationInt

import akka.persistence.typed.scaladsl.RetentionCriteria.snapshotEvery
import akka.cluster.Cluster
import io.github.positionpal.location.application.notifications.NotificationService
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.actor.typed.*
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import io.github.positionpal.location.tracking.utils.AkkaUtils.refOf
import io.github.positionpal.location.application.tracking.reactions.*
import io.github.positionpal.location.application.tracking.MapsService
import akka.actor.typed.SupervisorStrategy.restartWithBackoff
import io.github.positionpal.location.domain.UserState.*
import akka.cluster.sharding.typed.scaladsl.*
import cats.effect.IO
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.persistence.typed.PersistenceId
import io.github.positionpal.location.domain.EventConversions.userUpdateFrom
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.NotificationMessage
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
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

  /** A message sent by the actor to itself in response to an async operation or to initiate an action proactively. */
  sealed trait SelfMessage

  /** A [[SelfMessage]] indicating the spawned async operation has completed with an unuseful result. */
  case object Ignore extends SelfMessage

  /** A [[SelfMessage]] triggered regularly by a timer to check whether the user went offline. */
  case object AliveCheck extends SelfMessage

  type Observer = ActorRef[DrivenEvent]
  type Command = DrivingEvent | InternalEvent | SelfMessage
  type Event = StatefulDrivingEvent | InternalEvent

  case class StatefulDrivingEvent(state: UserState, event: DrivingEvent) extends AkkaSerializable
  object StatefulDrivingEvent:
    def from(session: Session, event: DrivingEvent): StatefulDrivingEvent =
      StatefulDrivingEvent(session.userState.next(event), event)
  end StatefulDrivingEvent

  def apply(using NotificationService[IO], MapsService[IO]): Entity[Command, ShardingEnvelope[Command]] =
    Entity(key)(ctx => this(ctx.entityId.splitted, tags(math.abs(ctx.entityId.hashCode % tags.size))))

  def apply(scope: Scope, tag: String)(using NotificationService[IO], MapsService[IO]): Behavior[Command] =
    Behaviors.setup: ctx =>
      given ActorContext[Command] = ctx
      Behaviors.withTimers: timer =>
        ctx.log.debug("Starting RealTimeUserTracker::{}@{}", scope, Cluster(ctx.system).selfMember.address)
        val persistenceId = PersistenceId(key.name, scope.concatenated)
        EventSourcedBehavior(persistenceId, Session.of(scope), commandHandler(timer), eventHandler)
          .withTagger(_ => Set(tag))
          .snapshotWhen((_, event, _) => event == RoutingStopped, deleteEventsOnSnapshot = true)
          .withRetention(snapshotEvery(numberOfEvents = 100, keepNSnapshots = 1).withDeleteEventsOnSnapshot)
          .onPersistFailure(restartWithBackoff(minBackoff = 2.second, maxBackoff = 15.seconds, randomFactor = 0.2))

  private def eventHandler: (Session, Event) => Session =
    (session, event) =>
      event match
        case StatefulDrivingEvent(_, e) => session.updateWith(e)
        case e: InternalEvent => session.updateWith(e)

  private def commandHandler(timer: TimerScheduler[Command])(using
      ActorContext[Command],
      NotificationService[IO],
      MapsService[IO],
  ): (Session, Command) => Effect[Event, Session] = (session, command) =>
    command match
      case e: InternalEvent => Effect.persist(e)
      case e: AliveCheck.type => aliveCheckHandler(timer)(session, e)
      case e: DrivingEvent =>
        if session.userState == Inactive then timer.startTimerAtFixedRate(msg = AliveCheck, interval = 20.seconds)
        trackingHandler(session, e)
      case _ => Effect.none

  import cats.effect.unsafe.implicits.global

  private def trackingHandler(using
      ctx: ActorContext[Command],
      notifier: NotificationService[IO],
      maps: MapsService[IO],
  ): (Session, DrivingEvent) => Effect[Event, Session] = (s, e) =>
    val reaction = (PreCheckNotifier[IO] >>> ArrivalCheck[IO] >>> StationaryCheck[IO] >>> ArrivalTimeoutCheck[IO])(s, e)
    ctx.pipeToSelf(reaction.unsafeToFuture()):
      case Success(result) =>
        result match
          case Left(e: (DrivingEvent | InternalEvent)) => e
          case _ => Ignore
      case Failure(exception) => ctx.log.error("Error while reacting: {}", exception.getMessage); Ignore
    persistAndNotify(e, s)(using ctx.system)

  private def aliveCheckHandler(timer: TimerScheduler[Command])(using
      ctx: ActorContext[Command],
      notifier: NotificationService[IO],
  ): (Session, AliveCheck.type) => Effect[Event, Session] = (s, _) =>
    if s.userState != Inactive && s.lastSampledLocation.get.timestamp.isBefore(now().minusSeconds(60))
    then
      timer.cancelAll()
      val event = WentOffline(s.lastSampledLocation.get.timestamp, s.scope)
      if s.userState == SOS || s.userState == Routing then
        val notification = NotificationMessage.create(
          s"${event.user.username()} connection lost!",
          s"User ${event.user.username()} went offline while in ${s.userState} mode!",
        )
        notifier.sendToGroup(s.scope.group, s.scope.user, notification).unsafeRunAndForget()
      persistAndNotify(event, s)(using ctx.system)
    else Effect.none

  private def persistAndNotify(event: DrivingEvent, session: Session)(using ActorSystem[?]): Effect[Event, Session] =
    Effect
      .persist(StatefulDrivingEvent.from(session, event))
      .thenRun(s => refOf(GroupManager.key, s.scope.group.value()) ! userUpdateFrom(event, s))
