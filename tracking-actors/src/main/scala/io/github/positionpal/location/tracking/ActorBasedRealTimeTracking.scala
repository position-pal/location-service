package io.github.positionpal.location.tracking

import io.github.positionpal.location.application.tracking.{MapsService, RealTimeTracking}

object ActorBasedRealTimeTracking extends RealTimeTracking:

  import akka.actor.typed.{ActorRef, ActorSystem}
  import akka.cluster.sharding.typed.scaladsl.ClusterSharding
  import cats.effect.kernel.Async
  import cats.effect.IO
  import cats.implicits.{toFlatMapOps, toFunctorOps}
  import io.github.positionpal.location.domain.*
  import io.github.positionpal.location.tracking.actors.RealTimeUserTracker
  import io.github.positionpal.location.presentation.ScopeUtils.concatenated
  import io.github.positionpal.location.application.notifications.NotificationService

  override type Outcome = DrivenEvent

  override type OutcomeObserver = Set[ActorRef[Outcome]]

  object Service:
    def apply[F[_]: Async](
        actorSystem: ActorSystem[?],
        notificationService: NotificationService[IO],
        mapsService: MapsService[IO],
    ): F[Service[F, Scope]] =
      for
        sharding <- Async[F].delay(ClusterSharding(actorSystem))
        _ <- Async[F].delay(sharding.init(RealTimeUserTracker(notificationService, mapsService)))
      yield ServiceImpl(actorSystem)

  private class ServiceImpl[F[_]: Async](actorSystem: ActorSystem[?]) extends Service[F, Scope]:
    override def handle(resource: Scope)(event: DrivingEvent): F[Unit] = Async[F].delay:
      refOf(resource) ! event

    override def addObserverFor(resource: Scope)(observer: OutcomeObserver): F[Unit] = Async[F].delay:
      observer.foreach(refOf(resource) ! RealTimeUserTracker.Wire(_))

    override def removeObserverFor(resource: Scope)(observer: OutcomeObserver): F[Unit] = Async[F].delay:
      observer.foreach(refOf(resource) ! RealTimeUserTracker.UnWire(_))

    private def refOf(scope: Scope) =
      ClusterSharding(actorSystem).entityRefFor(RealTimeUserTracker.key, scope.concatenated)
