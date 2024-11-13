package io.github.positionpal.location.infrastructure.services.projections

import scala.concurrent.Future

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{AtLeastOnceProjection, Handler}
import akka.projection.{ProjectionBehavior, ProjectionId}
import cats.effect.IO
import io.github.positionpal.location.application.storage.UserSessionWriter
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.Session.Snapshot
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker.Event

/** A projection that listens to the events emitted by the [[RealTimeUserTracker]]
  * actors and updates the user's session state, implementing the CQRS pattern.
  */
class UserSessionProjection:

  /** Initializes the projection for the sharded event sourced [[RealTimeUserTracker]] actor
    * deployed on the given [[system]] using as storage the provided [[UserSessionWriter]].
    */
  def init[T](system: ActorSystem[?], storage: UserSessionWriter[IO, T]): Unit =
    ShardedDaemonProcess(system).init(
      name = getClass.getSimpleName,
      RealTimeUserTracker.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, storage, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop),
    )

  private def createProjectionFor[T](
      system: ActorSystem[?],
      storage: UserSessionWriter[IO, T],
      index: Int,
  ): AtLeastOnceProjection[Offset, EventEnvelope[RealTimeUserTracker.Event]] =
    val tag = RealTimeUserTracker.tags(index)
    val sourceProvider = EventSourcedProvider.eventsByTag[RealTimeUserTracker.Event](
      system = system,
      readJournalPluginId = CassandraReadJournal.Identifier,
      tag = tag,
    )
    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId(getClass.getSimpleName, tag),
      sourceProvider,
      handler = () => UserSessionProjectionHandler(system, storage),
    )

/** The handler that processes the events emitted by the [[RealTimeUserTracker]] actor
  * updating the user's session state with the provided [[storage]].
  */
class UserSessionProjectionHandler[T](
    system: ActorSystem[?],
    storage: UserSessionWriter[IO, T],
) extends Handler[EventEnvelope[RealTimeUserTracker.Event]]:

  import io.github.positionpal.location.domain.EventConversions.given
  import cats.effect.unsafe.implicits.global

  override def process(envelope: EventEnvelope[Event]): Future[Done] =
    system.log.debug("Process envelope {}", envelope.event)
    envelope.event match
      case RealTimeUserTracker.StatefulDrivingEvent(state, event) =>
        val operation = event match
          case e: SampledLocation => storage.update(Snapshot(e.user, state, Some(e)))
          case e: RoutingStarted => storage.update(Snapshot(e.user, state, Some(e)))
          case e: SOSAlertTriggered => storage.update(Snapshot(e.user, state, Some(e)))
          case e: (SOSAlertStopped | WentOffline | RoutingStopped) =>
            storage.update(Snapshot(e.user, state, None))
        operation.map(_ => Done).unsafeToFuture()
      case _ => Future.successful(Done)
