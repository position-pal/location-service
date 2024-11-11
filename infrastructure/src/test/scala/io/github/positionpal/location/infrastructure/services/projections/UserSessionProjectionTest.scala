package io.github.positionpal.location.infrastructure.services.projections

import java.time.Instant

import scala.concurrent.Future

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.testkit.scaladsl.{ProjectionTestKit, TestProjection, TestSourceProvider}
import akka.stream.scaladsl.Source
import io.github.positionpal.location.application.storage.UserSessionReader
import io.github.positionpal.location.domain.{GPSLocation, SampledLocation, Session, UserId}
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
import org.scalatest.wordspec.AnyWordSpecLike

class UserSessionProjectionTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike {

  private val projectionTestKit = ProjectionTestKit(system)

  "The UserSessionProjection" should {
    "process events correctly" in {
      val repo = new UserSessionReader[Future] {
        override def sessionOf(userId: UserId): Future[Option[Session]] =
          println("[MOCK DB] sessionOf")
          Future.successful(None)
      }
      val handler = UserSessionProjectionHandler[Future]("tag", system, repo)
      val envents = Source(
        List[EventEnvelope[RealTimeUserTracker.Event]](
          createEnvelope(SampledLocation(Instant.now(), UserId("user1"), GPSLocation(0.0, 0.0)), 0L),
        ),
      )
      val projectId = ProjectionId("name", "key")
      val sourceProvider =
        TestSourceProvider[Offset, EventEnvelope[RealTimeUserTracker.Event]](envents, extractOffset = env => env.offset)
      val projection =
        TestProjection[Offset, EventEnvelope[RealTimeUserTracker.Event]](projectId, sourceProvider, () => handler)
      projectionTestKit.run(projection) {
        println("ASSERT FUNCTION")
        Thread.sleep(5_000)
      }
    }
  }

  private def createEnvelope(event: RealTimeUserTracker.Event, seqNo: Long, timestamp: Long = 0L) =
    EventEnvelope(Offset.sequence(seqNo), "persistenceId", seqNo, event, timestamp)
}
