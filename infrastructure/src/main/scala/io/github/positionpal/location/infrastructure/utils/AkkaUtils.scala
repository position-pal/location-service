package io.github.positionpal.location.infrastructure.utils

import akka.actor.BootstrapSetup
import akka.actor.typed.{ActorSystem, Behavior}
import cats.effect.Resource
import cats.effect.kernel.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.typesafe.config.Config

object AkkaUtils:

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
