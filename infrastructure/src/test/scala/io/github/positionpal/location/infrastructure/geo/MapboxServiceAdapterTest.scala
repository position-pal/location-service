package io.github.positionpal.location.infrastructure.geo

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.application.geo.Distance.DistanceUnit
import io.github.positionpal.location.application.geo.RoutingMode.Driving
import io.github.positionpal.location.application.geo.{Distance, MapsServiceError, RoutingMode}
import io.github.positionpal.location.domain.GPSLocation
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class MapboxServiceAdapterTest extends AnyFunSpec with Matchers:
  private val cesenaCampusLocation = GPSLocation(44.1476299926484, 12.2357184467018)
  private val bolognaCampusLocation = GPSLocation(44.487912, 11.32885)
  private val mapboxServiceAdapter = MapboxServiceAdapter()

  private given LoggerFactory[IO] = Slf4jFactory.create[IO]

  describe("The Mapbox service adapter"):
    it("should calculate the distance between two locations"):
      val distanceRequest = for
        config <- EmberClientBuilder.default[IO].build.use(client =>
          IO.pure(
            Configuration(
              client,
              "",
            ),
          ),
        )
        distance <- mapboxServiceAdapter.distance(Driving)(cesenaCampusLocation, bolognaCampusLocation).value
          .run(config)
      yield distance
      val result = distanceRequest.unsafeRunSync()
      println(result)
      result.isRight shouldBe true
      result.map(_.value should (be >= 90273.0 and be <= 90274.0))
      result.map(_.unit shouldBe DistanceUnit.Meters)

    it("should calculate the arrival time between two locations"):
      val arrivalTimeRequest = for
        config <- EmberClientBuilder.default[IO].build.use(client =>
          IO.pure(
            Configuration(
              client,
              "",
            ),
          ),
        )
        arrivalTime <- mapboxServiceAdapter.duration(Driving)(cesenaCampusLocation, bolognaCampusLocation).value
          .run(config)
      yield arrivalTime
      val result = arrivalTimeRequest.unsafeRunSync()
      println(result)
      result.isRight shouldBe true
      result.map(_.toMinutes should (be >= 75L))
