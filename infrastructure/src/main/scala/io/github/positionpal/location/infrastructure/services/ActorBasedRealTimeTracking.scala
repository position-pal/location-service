package io.github.positionpal.location.infrastructure.services

import io.github.positionpal.location.application.services.RealTimeTracking
import io.github.positionpal.location.domain.{DrivingEvent, GroupId}
import io.github.positionpal.location.infrastructure.services.actors.GroupManager

object ActorBasedRealTimeTracking extends RealTimeTracking:

  import akka.actor.typed.{ActorRef, ActorSystem}
  import akka.cluster.sharding.typed.scaladsl.ClusterSharding
  import cats.effect.kernel.Async
  import io.github.positionpal.location.infrastructure.services.actors.WebSocketsManagers.GroupWebsocketManager

  override type Outcome = GroupWebsocketManager.Command

  override type OutcomeObserver = ActorRef[Outcome]

  object Service:
    def apply[F[_]: Async](actorSystem: ActorSystem[?]): Service[F] = ServiceImpl(actorSystem)

  private class ServiceImpl[F[_]: Async](actorSystem: ActorSystem[?]) extends Service[F]:
    override def handleFor(groupId: GroupId)(event: DrivingEvent): F[Unit] = Async[F].delay:
      refOf(groupId) ! event

    override def addObserverFor(groupId: GroupId)(observer: OutcomeObserver): F[Unit] = Async[F].delay:
      refOf(groupId) ! GroupManager.Wire(observer)

    override def removeObserverFor(groupId: GroupId)(observer: OutcomeObserver): F[Unit] = Async[F].delay:
      refOf(groupId) ! GroupManager.UnWire(observer)

    private def refOf(groupId: GroupId) = ClusterSharding(actorSystem).entityRefFor(GroupManager.key, groupId.id)
