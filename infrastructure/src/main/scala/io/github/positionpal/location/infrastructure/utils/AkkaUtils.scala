package io.github.positionpal.location.infrastructure.utils

import akka.actor.BootstrapSetup
import akka.actor.typed.{ActorSystem, Behavior}
import cats.effect
import cats.effect.Resource
import cats.effect.kernel.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.typesafe.config.Config

object AkkaUtils:

  /** Starts an Akka actor system named [[actorSystemName]] with the given [[configuration]] and [[behavior]]. */
  def startup[F[_]: Async, T](
      configuration: Config,
      actorSystemName: String = "ClusterSystem",
  )(behavior: => Behavior[T]): Resource[F, ActorSystem[T]] =
    Resource:
      for
        ec <- Async[F].executionContext
        config <- Async[F].pure(BootstrapSetup(configuration).withDefaultExecutionContext(ec))
        system <- Async[F].delay(ActorSystem(behavior, actorSystemName, config))
        cancel = for
          _ <- Async[F].delay(system.terminate())
          _ <- Async[F].fromFuture(Async[F].delay(system.whenTerminated))
        yield ()
      yield (system, cancel)
