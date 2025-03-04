package io.github.positionpal.location.application.reactions

import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.domain.Distance.*
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.domain.TimeUtils.*
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.application.tracking.reactions.*
import io.github.positionpal.location.application.tracking.MapsService
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.RoutingMode.*
import cats.effect.IO
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.{GroupId, User, UserId}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.concurrent.Eventually.eventually

class TrackingEventReactionsTest extends AnyFunSpec with Matchers with MockFactory:

  private val scope = Scope(UserId.create("f789612f-4154-40b8-bdae-c86803a4dcfd"), GroupId.create("astro"))
  private val maps = mock[MapsService[IO]]
  when(maps.distance(_: RoutingMode)(_: GPSLocation, _: GPSLocation))
    .expects(*, *, *)
    .onCall((_, o, d) => if o == d then IO.pure(0.meters) else IO.pure(Double.MaxValue.meters))
    .anyNumberOfTimes
  private val groups = mock[UserGroupsService[IO]]
  when(groups.of)
    .expects(scope)
    .returning(IO.pure(Some(User.create(scope.userId, "Luke", "Skywalker", "luke@gmail.com"))))
    .anyNumberOfTimes
  private val notifier = mock[NotificationService[IO]]

  given MapsService[IO] = maps
  given NotificationService[IO] = notifier
  given UserGroupsService[IO] = groups

  describe("TrackingEventReactions"):
    describe("should `Continue`"):
      it("if no check is met"):
        val session = Session.from(scope, UserState.Active, None, None)
        val event = SampledLocation(now, scope.userId, scope.groupId, bolognaCampus.location)
        doChecks(session, event).unsafeRunSync() shouldBe Right(Continue)

    describe("should trigger a `Notification`"):
      it("if the user has started a journey and if the journey has ended"):
        val session = Session.from(scope, UserState.Active, None, None)
        expectNotification("Luke Skywalker is on their way to"):
          val event = RoutingStarted(now, scope, bolognaCampus.location, Driving, cesenaCampus, inTheFuture)
          doChecks(session, event).unsafeRunSync() shouldBe Left(())
        expectNotification("Luke Skywalker's journey has completed successfully"):
          val event = RoutingStopped(now, scope)
          doChecks(session, event).unsafeRunSync() shouldBe Left(())

      it("if the user has triggered an SOS and if the SOS has been stopped"):
        val session = Session.from(scope, UserState.Active, None, None)
        expectNotification("Luke Skywalker has triggered an SOS help request"):
          val sosAlertTriggeredEvent = SOSAlertTriggered(now, scope, bolognaCampus.location)
          doChecks(session, sosAlertTriggeredEvent).unsafeRunSync() shouldBe Left(())
        expectNotification("Luke Skywalker has stopped the SOS alarm"):
          val sosAlertStoppedEvent = SOSAlertStopped(now, scope)
          doChecks(session, sosAlertStoppedEvent).unsafeRunSync() shouldBe Left(())

      it("if the user goes offline and is in routing mode"):
        val session = Session.from(scope, UserState.Routing, None, None)
        val wentOfflineEvent = WentOffline(now, scope)
        expectNotification("Luke Skywalker went offline while on a journey"):
          doChecks(session, wentOfflineEvent).unsafeRunSync() shouldBe Left(())

      it("if the user is stuck in the same position for too long"):
        val event = SampledLocation(now, scope, bolognaCampus.location)
        val tracking = List
          .fill(50)(event)
          .foldLeft(
            Tracking.withMonitoring(RoutingMode.Driving, destination = cesenaCampus, estimatedArrival = inTheFuture),
          )(_ + _)
        val session = Session.from(scope, UserState.Routing, Some(event), Some(tracking))
        expectNotification("Luke Skywalker has been stuck in the same position"):
          doChecks(session, event).unsafeRunSync() should matchPattern { case Left(_: StuckAlertTriggered) => }

      it("if the user has not reached the destination within the expected time"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inThePast)
        val event = SampledLocation(now, scope, bolognaCampus.location)
        val session = Session.from(scope, UserState.Routing, Some(event), Some(tracking))
        expectNotification("Luke Skywalker has not reached their destination as expected"):
          doChecks(session, event).unsafeRunSync() should matchPattern { case Left(_: TimeoutAlertTriggered) => }

      it("if the user has arrived to the expected destination in time"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inTheFuture)
        val event = SampledLocation(now, scope, cesenaCampus.location)
        val session = Session.from(scope, UserState.Routing, Some(event), Some(tracking))
        expectNotification("Luke Skywalker has reached their destination on time"):
          doChecks(session, event).unsafeRunSync() should matchPattern { case Left(_: RoutingStopped) => }

  private def doChecks(session: Session, event: ClientDrivingEvent): IO[Outcome] =
    (PreCheckNotifier[IO] >>> ArrivalCheck[IO] >>> StationaryCheck[IO] >>> ArrivalTimeoutCheck[IO])(session, event)

  private def expectNotification(content: String)(testBlock: => Unit): Unit = eventually:
    when(notifier.sendToOwnGroup)
      .expects:
        where: (scope, n) =>
          scope.groupId == scope.groupId && scope.userId == scope.userId && n.body().contains(content)
      .returning(IO.unit)
      .once
    testBlock
