package io.github.positionpal.location.infrastructure.utils

import akka.actor.BootstrapSetup
import akka.actor.typed.{ActorSystem, Behavior}
import cats.effect.kernel.Async
import cats.effect.{IO, Resource}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.typesafe.config.Config

object AkkaUtils:

  def startup[T](
      configuration: Config,
      name: String = "ClusterSystem",
  )(behavior: => Behavior[T]): Resource[IO, ActorSystem[T]] =
    Resource:
      for
        ec <- IO.executionContext
        config = BootstrapSetup(configuration).withDefaultExecutionContext(ec)
        system <- IO(ActorSystem(behavior, name, config))
        cancel = IO.fromFuture(IO(system.whenTerminated)).void
      yield (system, cancel)

  def startup2[T, F[_]: Async](
      configuration: Config,
      name: String = "ClusterSystem",
  )(behavior: => Behavior[T]): Resource[F, ActorSystem[T]] =
    Resource:
      for
        ec <- Async[F].executionContext
        config <- Async[F].pure(BootstrapSetup(configuration).withDefaultExecutionContext(ec))
        system <- Async[F].delay(ActorSystem(behavior, name, config))
        cancel = Async[F].fromFuture(Async[F].delay(system.whenTerminated)).void
      yield (system, cancel)
