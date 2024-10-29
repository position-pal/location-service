package io.github.positionpal.location.infrastructure.services

import io.github.positionpal.location.application.services.RealTimeTracking
import io.github.positionpal.location.domain.{DrivingEvent, UserId}
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker

object ActorBasedRealTimeTracking extends RealTimeTracking:

  import akka.actor.typed.{ActorRef, ActorSystem}
  import akka.cluster.sharding.typed.scaladsl.ClusterSharding
  import cats.effect.kernel.Async
  import io.github.positionpal.location.infrastructure.ws.WebSockets

  override type Outcome = WebSockets.Protocol

  override type OutcomeObserver = Set[ActorRef[Outcome]]

  object Service:
    def apply[F[_]: Async](actorSystem: ActorSystem[?]): Service[F, UserId] = ServiceImpl(actorSystem)

  private class ServiceImpl[F[_]: Async](actorSystem: ActorSystem[?]) extends Service[F, UserId]:
    override def handle(event: DrivingEvent): F[Unit] = Async[F].delay:
      refOf(event.user) ! event

    override def addObserverFor(resource: UserId)(observer: OutcomeObserver): F[Unit] = Async[F].delay:
      observer.foreach(refOf(resource) ! RealTimeUserTracker.Wire(_))

    override def removeObserverFor(resource: UserId)(observer: OutcomeObserver): F[Unit] = Async[F].delay:
      observer.foreach(refOf(resource) ! RealTimeUserTracker.UnWire(_))

    private def refOf(userId: UserId) = ClusterSharding(actorSystem).entityRefFor(RealTimeUserTracker.key, userId.id)
