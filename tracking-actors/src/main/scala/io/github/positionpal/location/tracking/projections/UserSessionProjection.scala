package io.github.positionpal.location.tracking.projections

import scala.concurrent.Future

import akka.actor.typed.ActorSystem
import io.github.positionpal.location.tracking.actors.RealTimeUserTracker
import akka.projection.{ProjectionBehavior, ProjectionId}
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.tracking.actors.RealTimeUserTracker.Event
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import cats.effect.IO
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.Done
import akka.persistence.query.Offset
import akka.projection.scaladsl.{AtLeastOnceProjection, Handler}
import akka.projection.eventsourced.EventEnvelope
import io.github.positionpal.location.application.sessions.UserSessionsWriter
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

/** A projection that listens to the events emitted by the [[RealTimeUserTracker]]
  * actors and updates the user's session state, implementing the CQRS pattern.
  */
object UserSessionProjection:

  /** Initializes the projection for the sharded event sourced [[RealTimeUserTracker]] actor
    * deployed on the given [[system]] using as storage the provided [[UserSessionsWriter]].
    */
  def init[T](system: ActorSystem[?], storage: UserSessionsWriter[IO, T]): Unit =
    ShardedDaemonProcess(system).init(
      name = getClass.getSimpleName,
      RealTimeUserTracker.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, storage, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop),
    )

  private def createProjectionFor[T](
      system: ActorSystem[?],
      storage: UserSessionsWriter[IO, T],
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
    val storage: UserSessionsWriter[IO, T],
) extends Handler[EventEnvelope[RealTimeUserTracker.Event]]:

  import cats.effect.unsafe.implicits.global
  import io.github.positionpal.location.domain.EventConversions.given
  import io.github.positionpal.location.domain.Session.Snapshot

  override def process(envelope: EventEnvelope[Event]): Future[Done] =
    system.log.debug("Process envelope {}", envelope.event)
    envelope.event match
      case RealTimeUserTracker.StatefulDrivingEvent(state, event) =>
        val operation = event match
          case e: SampledLocation => storage.update(Snapshot(e.scope, state, Some(e)))
          case e: SOSAlertTriggered => storage.update(Snapshot(e.scope, state, Some(e)))
          case e: RoutingStarted =>
            storage.addRoute(e.scope, e.mode, e.destination, e.expectedArrival) >>
              storage.update(Snapshot(e.scope, state, Some(e)))
          case e: (SOSAlertStopped | RoutingStopped) =>
            storage.removeRoute(e.scope) >> storage.update(Snapshot(e.scope, state, None))
          case e @ _ => storage.update(Snapshot(e.scope, state, None))
        operation
          .handleErrorWith(err => IO(system.log.error("Persistent projection fatal error: {}", err)))
          .map(_ => Done)
          .unsafeToFuture()
