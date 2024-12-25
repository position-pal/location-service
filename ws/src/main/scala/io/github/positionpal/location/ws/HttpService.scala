package io.github.positionpal.location.ws

import io.github.positionpal.location.ws.handlers.v1.V1RoutesHandler
import io.github.positionpal.location.ws.routes.v1.V1Routes

object HttpService:

  /** Configuration for the HTTP service. */
  trait Configuration:
    /** The port the HTTPS services should listen on. */
    def port: Int

  object Configuration:

    import cats.effect.kernel.Sync
    import cats.data.ValidatedNec
    import io.github.positionpal.location.commons.ConfigurationError
    import io.github.positionpal.location.commons.ConfigurationError.*
    import io.github.positionpal.location.commons.ScopeFunctions.*
    import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxValidatedIdBinCompat0}

    /** Create a new [[Configuration]] instance.
      * @param port the port the server should listen on.
      * @return a [[Validated]] instance containing either a valid [[Configuration]] or a [[ConfigurationError]].
      */
    def apply[F[_]: Sync](port: Int): F[ValidatedNec[ConfigurationError, Configuration]] = Sync[F].delay:
      port.positive.map(BasicHttpConfiguration.apply)
    .handleError(e => Invalid(e.toString).invalidNec)

    /** Create a new [[Configuration]] instance with the parameters read from environment variables,
      * expected in `HTTP_<PARAMETER>` format.
      * @return a [[Validated]] instance containing either a valid [[Configuration]] or a [[ConfigurationError]].
      */
    def fromEnv[F[_]: Sync]: F[ValidatedNec[ConfigurationError, Configuration]] = Sync[F].delay:
      "HTTP_PORT".let(s => sys.env.get(s).validStr(s).andThen(_.toInt.positive)).map(BasicHttpConfiguration.apply)
    .handleError(e => Invalid(e.toString).invalidNec)

    private case class BasicHttpConfiguration(port: Int) extends Configuration

  import akka.actor.typed.ActorSystem
  import akka.http.scaladsl.Http
  import cats.effect.{Async, IO, Resource}
  import cats.implicits.toFunctorOps
  import io.github.positionpal.location.domain.Scope
  import io.github.positionpal.location.tracking.ActorBasedRealTimeTracking

  def start[F[_]: Async](configuration: Configuration)(service: ActorBasedRealTimeTracking.Service[IO, Scope])(using
      actorSystem: ActorSystem[?],
  ): Resource[F, Http.ServerBinding] =
    Resource.make(
      Async[F].fromFuture:
        Async[F].delay:
          Http(actorSystem.classicSystem).newServerAt("localhost", configuration.port)
            .bind(V1Routes(V1RoutesHandler(service)).versionedRoutes),
    )(binding => Async[F].fromFuture(Async[F].delay(binding.unbind())).void)
