package io.github.positionpal.location.infrastructure.services

import scala.concurrent.ExecutionContext

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cats.effect.kernel.Resource.eval
import cats.effect.{Async, Resource}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import cats.mtl.{Ask, Stateful}
import com.typesafe.config.Config
import io.github.positionpal.location.application.services.{RealTimeTrackingService, StartableOps}
import io.github.positionpal.location.commons.{CanAsk, HasState}
import io.github.positionpal.location.domain.DomainEvent
import io.github.positionpal.location.infrastructure.utils.AkkaUtils

object RealTimeTrackingService:

  def apply[F[_]: Async: CanAsk[Config]: HasState[ActorSystem[DomainEvent]]](using
      ExecutionContext,
  ): RealTimeTrackingService[F] =
    ActorBasedRealTimeTrackingService()

  private class ActorBasedRealTimeTrackingService[F[_]: Async: CanAsk[Config]: HasState[ActorSystem[DomainEvent]]]
      extends RealTimeTrackingService[F]
      with StartableOps[[A] =>> Resource[F, A], ActorSystem[DomainEvent]]:

    override def start: Resource[F, ActorSystem[DomainEvent]] =
      for
        config <- eval(Ask[F, Config].ask)
        system <- AkkaUtils.startup2(config)(Behaviors.empty)
        cluster <- eval(Async[F].delay(ClusterSharding(system)))
        _ <- eval(Stateful[F, ActorSystem[DomainEvent]].modify(_ => system))
        _ <- eval(Async[F].delay(cluster.init(RealTimeUserTracker())))
      yield system

    override def handle(event: DomainEvent): F[Unit] =
      for
        actorSystem <- Stateful[F, ActorSystem[DomainEvent]].get
        _ <- Async[F].delay:
          ClusterSharding(actorSystem).entityRefFor(RealTimeUserTracker.key, event.user.id) ! event
      yield ()
