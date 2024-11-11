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
import akka.projection.scaladsl.{AtLeastOnceProjection, Handler, SourceProvider}
import akka.projection.{ProjectionBehavior, ProjectionId}
import io.github.positionpal.location.application.storage.UserSessionReader
import io.github.positionpal.location.domain.UserId
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker.Event

class UserSessionProjection[F[_]] {

  def init(system: ActorSystem[?], repository: UserSessionReader[F]): Unit =
    ShardedDaemonProcess(system).init(
      name = getClass.getSimpleName,
      RealTimeUserTracker.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop),
    )

  private def createProjectionFor(
      system: ActorSystem[?],
      repository: UserSessionReader[F],
      index: Int,
  ): AtLeastOnceProjection[Offset, EventEnvelope[RealTimeUserTracker.Event]] = {
    val tag = RealTimeUserTracker.tags(index)
    val sourceProvider: SourceProvider[Offset, EventEnvelope[RealTimeUserTracker.Event]] =
      EventSourcedProvider.eventsByTag[RealTimeUserTracker.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = tag,
      )
    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId(getClass.getSimpleName, tag),
      sourceProvider,
      handler = () => UserSessionProjectionHandler(tag, system, repository),
    )
  }
}

class UserSessionProjectionHandler[F[_]](
    tag: String,
    system: ActorSystem[?],
    storage: UserSessionReader[F],
) extends Handler[EventEnvelope[RealTimeUserTracker.Event]]:

  override def process(envelope: EventEnvelope[Event]): Future[Done] =
    storage.sessionOf(UserId("user1"))
    system.log.debug("Process envelope {} with tag {}", envelope.event, tag)
    Future.successful(Done)
