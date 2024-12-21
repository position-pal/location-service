package io.github.positionpal.location.ws

object HttpService:

  import akka.actor.typed.ActorSystem
  import akka.http.scaladsl.Http
  import cats.effect.{Async, IO, Resource}
  import cats.implicits.toFunctorOps
  import io.github.positionpal.location.domain.Scope
  import io.github.positionpal.location.tracking.ActorBasedRealTimeTracking

  def start[F[_]: Async](port: Int)(service: ActorBasedRealTimeTracking.Service[IO, Scope])(using
      actorSystem: ActorSystem[?],
  ): Resource[F, Http.ServerBinding] =
    Resource.make(
      Async[F].fromFuture:
        Async[F].delay:
          Http(actorSystem.classicSystem).newServerAt("localhost", port).bind(WebSockets.Routes.groupRoute(service)),
    )(binding => Async[F].fromFuture(Async[F].delay(binding.unbind())).void)
