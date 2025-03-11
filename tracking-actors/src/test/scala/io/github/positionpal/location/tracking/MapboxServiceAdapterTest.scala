package io.github.positionpal.location.tracking

import io.github.positionpal.location.domain.Distance.DistanceUnit
import io.github.positionpal.location.tracking.utils.HTTPUtils
import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.client.Client
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.location.domain.GeoUtils.*
import org.scalatest.funspec.AnyFunSpec
import io.github.positionpal.location.commons.EnvVariablesProvider
import io.github.positionpal.location.domain.RoutingMode.Driving
import io.circe.parser.*
import cats.effect.{IO, Resource}

class MapboxServiceAdapterTest extends AnyFunSpec with Matchers:

  import MapboxServiceAdapterTest.*

  describe("The Mapbox service adapter"):
    it("should calculate the distance between two locations"):
      val distance = (for
        mapService <- mapServiceAdapter
        dist <- mapService.distance(Driving)(cesenaCampus.position, bolognaCampus.position)
      yield dist).unsafeRunSync()
      // distance depends on the route (selected depending on the traffic)!
      distance.value should (be >= 80_000.0 and be <= 100_000.0)
      distance.unit shouldBe DistanceUnit.Meters

    it("should calculate the arrival time between two locations"):
      val arrivalTime = (for
        mapService <- mapServiceAdapter
        time <- mapService.duration(Driving)(cesenaCampus.position, bolognaCampus.position)
      yield time).unsafeRunSync()
      arrivalTime.toMinutes should be >= 60L // arrival time depends on the traffic!

object MapboxServiceAdapterTest:

  private val mapServiceAdapter =
    for
      envs <- EnvVariablesProvider[IO].configuration
      tokenIsPreset = envs.contains("MAPBOX_API_KEY")
      config <-
        if tokenIsPreset then
          HTTPUtils.clientRes.use(client => IO.pure(MapboxService.Configuration(client, envs("MAPBOX_API_KEY"))))
        else HTTPUtils.clientRes.use(client => IO.pure(MapboxService.Configuration(mockedClient(client), "fake-token")))
      mapService <- MapboxService[IO](config)
    yield mapService

  @SuppressWarnings(Array("scalafix:DisableSyntax.asInstanceOf"))
  private def mockedClient(delegatingClient: Client[IO]): Client[IO] =
    new Client[IO]:
      export delegatingClient.{expect as _, run as _, *}
      override def run(req: Request[IO]): Resource[IO, Response[IO]] = Resource.pure(Response[IO](Status.Ok))
      override def expect[A](uri: Uri)(implicit d: EntityDecoder[IO, A]): IO[A] =
        IO.pure(partialMockedMapboxResponse.asInstanceOf[A])
      // The following methods are not used in the tests
      override def expect[A](s: String)(implicit d: EntityDecoder[IO, A]): IO[A] = ???
      override def expect[A](req: Request[IO])(implicit d: EntityDecoder[IO, A]): IO[A] = ???
      override def expect[A](req: IO[Request[IO]])(implicit d: EntityDecoder[IO, A]): IO[A] = ???

  /** A partial mocked Mapbox response containing only the routes information needed for the tests. */
  private val partialMockedMapboxResponse = parse:
    """
      |{
      |   "routes":[
      |      {
      |         "weight_name":"auto",
      |         "weight":5706.868,
      |         "duration":4934.365,
      |         "distance":90393.367
      |      }
      |   ],
      |   "code":"Ok"
      |}
      |""".stripMargin
  .getOrElse(throw IllegalStateException("Malformed mocked mapbox response"))
