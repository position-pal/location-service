package io.github.positionpal.location.application.reactions

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.application.tracking.reactions.*
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.Distance.*
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.RoutingMode.*
import io.github.positionpal.location.domain.TimeUtils.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TrackingEventReactionsTest extends AnyFunSpec with Matchers with MockFactory:

  private val scope = Scope(UserId.create("luke"), GroupId.create("astro"))
  private val maps = mock[MapsService[IO]]
  when(maps.distance(_: RoutingMode)(_: GPSLocation, _: GPSLocation))
    .expects(*, *, *)
    .onCall((_, o, d) => if o == d then IO.pure(0.meters) else IO.pure(Double.MaxValue.meters))
    .anyNumberOfTimes
  private val notifier = mock[NotificationService[IO]]

  given MapsService[IO] = maps
  given NotificationService[IO] = notifier

  describe("TrackingEventReactions"):
    describe("should `Continue`"):
      it("if no check is met"):
        val session = Session.from(scope, UserState.Active, None, None)
        val event = SampledLocation(now, scope.user, bolognaCampus)
        doChecks(session, event).unsafeRunSync() shouldBe Right(Continue)

    describe("should trigger a `Notification`"):
      it("if the user has started a journey and if the journey has ended"):
        val session = Session.from(scope, UserState.Active, None, None)
        expectNotification("luke is on their way to"):
          val routingStartedEvent = RoutingStarted(now, scope.user, bolognaCampus, Driving, cesenaCampus, inTheFuture)
          doChecks(session, routingStartedEvent).unsafeRunSync() shouldBe Left(())
        expectNotification("luke journey completed successfully"):
          val routingStoppedEvent = RoutingStopped(now, scope.user)
          doChecks(session, routingStoppedEvent).unsafeRunSync() shouldBe Left(())

      it("if the user has triggered an SOS and if the SOS has been stopped"):
        val session = Session.from(scope, UserState.Active, None, None)
        expectNotification("luke has triggered an SOS help request"):
          val sosAlertTriggeredEvent = SOSAlertTriggered(now, scope.user, bolognaCampus)
          doChecks(session, sosAlertTriggeredEvent).unsafeRunSync() shouldBe Left(())
        expectNotification("luke has stopped the SOS alarm"):
          val sosAlertStoppedEvent = SOSAlertStopped(now, scope.user)
          doChecks(session, sosAlertStoppedEvent).unsafeRunSync() shouldBe Left(())

      it("if the user goes offline and is in routing mode"):
        val session = Session.from(scope, UserState.Routing, None, None)
        val wentOfflineEvent = WentOffline(now, scope.user)
        expectNotification("luke went offline while on a journey"):
          doChecks(session, wentOfflineEvent).unsafeRunSync() shouldBe Left(())

      it("if the user is stuck in the same position for too long"):
        val event = SampledLocation(now, scope.user, bolognaCampus)
        val tracking = List.fill(50)(event).foldLeft(
          Tracking.withMonitoring(RoutingMode.Driving, arrivalLocation = cesenaCampus, estimatedArrival = inTheFuture),
        )(_ + _)
        val session = Session.from(scope, UserState.Routing, Some(event), Some(tracking))
        expectNotification("luke has been stuck in the same position"):
          doChecks(session, event).unsafeRunSync() shouldBe Left(())

      it("if the user has not reached the destination within the expected time"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inThePast)
        val event = SampledLocation(now, scope.user, bolognaCampus)
        val session = Session.from(scope, UserState.Routing, Some(event), Some(tracking))
        expectNotification("luke has not reached their destination as expected"):
          doChecks(session, event).unsafeRunSync() shouldBe Left(())

      it("if the user has arrived to the expected destination in time"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inTheFuture)
        val event = SampledLocation(now, scope.user, cesenaCampus)
        val session = Session.from(scope, UserState.Routing, Some(event), Some(tracking))
        expectNotification("luke has reached their destination on time"):
          doChecks(session, event).unsafeRunSync() should matchPattern { case Left(_: RoutingStopped) => }

  private def doChecks(session: Session, event: DrivingEvent): IO[Outcome] =
    (EventPreCheckNotifier[IO] >>> ArrivalCheck[IO] >>> StationaryCheck[IO] >>> ArrivalTimeoutCheck[IO])(session, event)

  private def expectNotification(content: String)(testBlock: => Unit): Unit =
    when(notifier.sendToGroup)
      .expects(where((guid, uid, n) => guid == scope.group && uid == scope.user && n.body().contains(content)))
      .returning(IO.unit)
      .once
    testBlock
