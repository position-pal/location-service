package io.github.positionpal.location.tracking

import io.github.positionpal.location.application.tracking.MapsService
import org.http4s.client.Client
import cats.effect.kernel.Async

/** A [[MapService]] adapter interacting with the Mapbox service.
  * @see <a href="https://docs.mapbox.com/api/navigation/directions/">Mapbox Directions API</a>.
  */
object MapboxService:

  /** Configuration for the [[MapboxService]].
    * @param client      the [[Client]] to use it for HTTP requests
    * @param accessToken the access token to authenticate with the Mapbox service
    */
  case class Configuration[F[_]](client: Client[F], accessToken: String)

  def apply[F[_]: Async](configuration: Configuration[F]): F[MapsService[F]] =
    Async[F].pure(AdapterImpl[F](configuration))

  private class AdapterImpl[F[_]: Async](config: Configuration[F]) extends MapsService[F]:

    import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
    import io.github.positionpal.location.domain.{Distance, GPSLocation, RoutingMode}
    import io.github.positionpal.location.domain.Distance.meters
    import io.github.positionpal.location.domain.RoutingMode.Driving
    import io.circe.Json
    import org.http4s.circe.jsonOf
    import org.http4s.implicits.uri
    import org.http4s.Uri
    import scala.concurrent.duration.{DurationDouble, FiniteDuration}

    override def duration(mode: RoutingMode)(origin: GPSLocation, destination: GPSLocation): F[FiniteDuration] =
      req[FiniteDuration](mode, origin, destination)(_.extractDuration)

    override def distance(mode: RoutingMode)(origin: GPSLocation, destination: GPSLocation): F[Distance] =
      req[Distance](mode, origin, destination)(_.extractDistance)

    private def req[T](mode: RoutingMode, origin: GPSLocation, destination: GPSLocation)(extract: Json => F[T]): F[T] =
      for
        res <- config.client.expect(dirRoute(mode, origin, destination, config.accessToken))(jsonOf[F, Json]).attempt
        json <- Async[F].fromEither(res).flatMap(extract(_))
      yield json

    private def dirRoute(mode: RoutingMode, origin: GPSLocation, destination: GPSLocation, token: String): Uri =
      val smode = mode.toString.toLowerCase.appendedAll(if mode == Driving then "-traffic" else "")
      uri"https://api.mapbox.com/directions/v5/mapbox/"
        .addPath(smode)
        .addSegment(s"${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}")
        .withQueryParam("access_token", token)

    extension (json: Json)
      private def extractDistance: F[Distance] =
        Async[F].fromEither(json.routes.downField("distance").as[Double].map(_.meters))

      private def extractDuration: F[FiniteDuration] =
        Async[F].fromEither(json.routes.downField("duration").as[Double].map(_.seconds))

      private def routes = json.hcursor.downField("routes").downArray
