package io.github.positionpal.location.infrastructure

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import cats.effect.kernel.Async
import cats.effect.{IO, Resource}
import cats.implicits.toFunctorOps
import com.typesafe.config.{Config, ConfigFactory}
import io.github.positionpal.location.domain.UserId
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import io.github.positionpal.location.infrastructure.ws.WebSockets

object EndOfWorld:

  @main def main1(): Unit =
    import cats.effect.unsafe.implicits.global
    startup(8080)(ConfigFactory.load("akka.conf")).use(_ => IO.never).unsafeRunSync()

  @main def main2(): Unit =
    import cats.effect.unsafe.implicits.global
    val config = ConfigFactory.parseString("akka.remote.artery.canonical.port = 2552")
      .withFallback(ConfigFactory.load("akka.conf"))
    startup(8081)(config).use(_ => IO.never).unsafeRunSync()

  def startup(port: Int)(config: Config): Resource[IO, ActorSystem[Any]] =
    for
      actorSystem <- AkkaUtils.startup[IO, Any](config)(Behaviors.empty)
      given ActorSystem[Any] = actorSystem
      realTimeTrackingService <- Resource.eval(ActorBasedRealTimeTracking.Service[IO](actorSystem))
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
