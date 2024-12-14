package io.github.positionpal.location.tracking

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.commons.EnvVariablesProvider
import io.github.positionpal.location.domain.Distance.DistanceUnit
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.RoutingMode.Driving
import io.github.positionpal.location.tracking.utils.HTTPUtils
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MapboxServiceAdapterTest extends AnyFunSpec with Matchers:

  private val mapServiceAdapter =
    for
      envs <- EnvVariablesProvider[IO].configuration
      config <- HTTPUtils.clientRes.use(client => IO.pure(MapboxService.Configuration(client, envs("MAPBOX_API_KEY"))))
      mapService <- MapboxService[IO](config)
    yield mapService

  describe("The Mapbox service adapter"):
    it("should calculate the distance between two locations"):
      val distance = (for
        mapService <- mapServiceAdapter
        dist <- mapService.distance(Driving)(cesenaCampus, bolognaCampus)
      yield dist).unsafeRunSync()
      // distance depends on the route (selected depending on the traffic)!
      distance.value should (be >= 80_000.0 and be <= 100_000.0)
      distance.unit shouldBe DistanceUnit.Meters

    it("should calculate the arrival time between two locations"):
      val arrivalTime = (for
        mapService <- mapServiceAdapter
        time <- mapService.duration(Driving)(cesenaCampus, bolognaCampus)
      yield time).unsafeRunSync()
      arrivalTime.toMinutes should be >= 60L // arrival time depends on the traffic!
