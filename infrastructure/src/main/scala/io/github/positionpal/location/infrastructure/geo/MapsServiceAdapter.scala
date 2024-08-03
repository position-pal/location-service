package io.github.positionpal.location.infrastructure.geo

import java.util.Date

import cats.data.{EitherT, ReaderT}
import cats.effect.IO
import cats.implicits.catsSyntaxEither
import io.circe.Json
import io.github.positionpal.location.application.geo.{Distance, MapsService, MapsServiceError, RoutingMode}
import io.github.positionpal.location.domain.GPSLocation
import org.http4s.Uri
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri

case class Configuration(client: Client[IO], accessToken: String)

type IOWithContext[E] = ReaderT[IO, Configuration, E]
type Response[E] = EitherT[IOWithContext, MapsServiceError, E]

/** A [[MapService]] adapter interacting with the Mapbox service. */
class MapboxServiceAdapter extends MapsService[Response]:

  override def arrivalTime(mode: RoutingMode)(origin: GPSLocation, destination: GPSLocation): Response[Date] = ???

  override def distance(mode: RoutingMode)(origin: GPSLocation, destination: GPSLocation): Response[Distance] =
    EitherT:
      ReaderT: configuration =>
        configuration.client
          .expect(directionsApiRoute(mode, origin, destination, configuration.accessToken))(jsonOf[IO, Json]).attempt
          .map(_.leftMap(_.getMessage)).map(_.flatMap(_.extractDistance))

  private def directionsApiRoute(mode: RoutingMode, origin: GPSLocation, destination: GPSLocation, token: String): Uri =
    uri"https://api.mapbox.com/directions/v5/mapbox/".addPath(mode.toString.toLowerCase)
      .addSegment(s"${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}")
      .withQueryParam("access_token", token)

  import io.github.positionpal.location.presentation.geo.Distance.distancePlainDecoder

  extension (json: Json)
    private def extractDistance: Either[String, Distance] =
      json.hcursor.downField("routes").downArray.downField("distance").as[Distance].left.map(_.message)
