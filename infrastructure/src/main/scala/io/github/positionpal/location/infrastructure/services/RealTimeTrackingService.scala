package io.github.positionpal.location.infrastructure.services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cats.effect.kernel.Resource.eval
import cats.effect.{Async, Resource}
import cats.mtl.Ask
import com.typesafe.config.Config
import io.github.positionpal.location.application.services.RealTimeTrackingService
import io.github.positionpal.location.commons.CanAsk
import io.github.positionpal.location.domain.DomainEvent
import io.github.positionpal.location.infrastructure.utils.AkkaUtils

object RealTimeTrackingService:

  /** Creates an Akka Actor based [[RealTimeTrackingService]]. */
  def apply[F[_]: Async: CanAsk[Config]]: Resource[F, RealTimeTrackingService[F] & AkkaActorSystemProvider[Any]] =
    for
      config <- eval(Ask[F, Config].ask)
      system <- AkkaUtils.startup(config)(Behaviors.empty)
      cluster <- eval(Async[F].delay(ClusterSharding(system)))
      _ <- eval(Async[F].delay(cluster.init(RealTimeUserTracker())))
    yield ActorBasedRealTimeTrackingService[F](system)

  private class ActorBasedRealTimeTrackingService[F[_]: Async](override val system: ActorSystem[Any])
      extends RealTimeTrackingService[F]
      with AkkaActorSystemProvider[Any]:

    override def handle(event: DomainEvent): F[Unit] =
      Async[F].delay:
        ClusterSharding(system).entityRefFor(RealTimeUserTracker.key, event.user.id) ! event
