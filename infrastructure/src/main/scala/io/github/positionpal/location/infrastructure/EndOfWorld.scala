package io.github.positionpal.location.infrastructure

import scala.concurrent.Future

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import cats.effect.kernel.Async
import cats.effect.{IO, Resource}
import cats.implicits.toFunctorOps
import com.typesafe.config.Config
import io.github.positionpal.location.application.storage.UserSessionReader
import io.github.positionpal.location.domain.{Session, UserId}
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.services.projections.UserSessionProjection
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import io.github.positionpal.location.infrastructure.ws.WebSockets

object EndOfWorld:

  def startup(port: Int)(config: Config): Resource[IO, ActorSystem[Any]] =
    for
      actorSystem <- AkkaUtils.startup[IO, Any](config)(Behaviors.empty)
      given ActorSystem[Any] = actorSystem
      realTimeTrackingService <- Resource.eval(ActorBasedRealTimeTracking.Service[IO](actorSystem))
      _ <- configureProjection[IO]
      _ <- configureHttpServer[IO](port)(realTimeTrackingService)
    yield actorSystem

  def configureHttpServer[F[_]: Async](port: Int)(
      service: ActorBasedRealTimeTracking.Service[IO, UserId],
  )(using actorSystem: ActorSystem[?]): Resource[F, Http.ServerBinding] =
    Resource.make(
      Async[F].fromFuture:
        Async[F].delay:
          Http(actorSystem.classicSystem).newServerAt("localhost", port).bind(WebSockets.Routes.groupRoute(service)),
    )(binding => Async[F].fromFuture(Async[F].delay(binding.unbind())).void)

  def configureProjection[F[_]: Async](using actorSystem: ActorSystem[?]): Resource[F, Unit] =
    val repo = new UserSessionReader[Future] {
      override def sessionOf(userId: UserId): Future[Option[Session]] =
        println("[MOCK DB] sessionOf")
        Future.successful(None)
    }
    Resource.eval(Async[F].delay(UserSessionProjection().init(actorSystem, repo)))
