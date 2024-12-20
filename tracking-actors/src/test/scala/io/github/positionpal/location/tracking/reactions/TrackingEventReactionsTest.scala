package io.github.positionpal.location.tracking.reactions

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.positionpal.entities.UserId
import io.github.positionpal.location.application.reactions.*
import io.github.positionpal.location.application.reactions.TrackingEventReaction.{Continue, Notification}
import io.github.positionpal.location.commons.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.TimeUtils.*
import io.github.positionpal.location.tracking.MapboxService
import io.github.positionpal.location.tracking.utils.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TrackingEventReactionsTest extends AnyFunSpec with Matchers:

  private val testUser: UserId = UserId.create("luke")

  describe("TrackingEventReactions"):
    describe("should `Continue`"):
      it("if no check is met"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inTheFuture)
        val event: SampledLocation = SampledLocation(now, testUser, bolognaCampus)
        checksFor(tracking, event).unsafeRunSync() should matchPattern { case Right(Continue) => }

    describe("should trigger a `Notification`"):
      it("if the user is stuck in the same position for too long"):
        val tracking = List.fill(20)(SampledLocation(now, testUser, bolognaCampus)).foldLeft(
          Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inTheFuture),
        )((tracking, sample) => tracking.addSample(sample))
        val event: SampledLocation = SampledLocation(now, testUser, bolognaCampus)
        checksFor(tracking, event).unsafeRunSync() should matchPattern { case Left(Notification.Alert(_)) => }

      it("if the user has not reached the destination within the expected time"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inThePast)
        val event: SampledLocation = SampledLocation(now, testUser, bolognaCampus)
        checksFor(tracking, event).unsafeRunSync() should matchPattern { case Left(Notification.Alert(_)) => }

      it("if the user has arrived to the expected destination in time"):
        val tracking = Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, inTheFuture)
        val event: SampledLocation = SampledLocation(now, testUser, cesenaCampus)
        checksFor(tracking, event).unsafeRunSync() should matchPattern { case Left(Notification.Success(_)) => }

  private def checksFor(tracking: MonitorableTracking, event: SampledLocation) =
    for
      envs <- EnvVariablesProvider[IO].configuration
      config <- HTTPUtils.clientRes.use(client => IO.pure(MapboxService.Configuration(client, envs("MAPBOX_API_KEY"))))
      mapService <- MapboxService[IO](config)
      check = ArrivalCheck(mapService) >>> StationaryCheck() >>> ArrivalTimeoutCheck()
      result <- check(tracking, event)
    yield result