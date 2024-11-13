package io.github.positionpal.location.infrastructure.services.projections

import scala.collection.concurrent.TrieMap

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.testkit.scaladsl.{ProjectionTestKit, TestProjection, TestSourceProvider}
import akka.stream.scaladsl.Source
import cats.effect.IO
import io.github.positionpal.location.application.storage.UserSessionWriter
import io.github.positionpal.location.domain.EventConversions.given
import io.github.positionpal.location.domain.RoutingMode.Driving
import io.github.positionpal.location.domain.Session.Snapshot
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.domain.{RoutingStarted, SampledLocation, Session, UserId}
import io.github.positionpal.location.infrastructure.GeoUtils.*
import io.github.positionpal.location.infrastructure.TimeUtils.*
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker.StatefulDrivingEvent
import org.scalatest.wordspec.AnyWordSpecLike

class UserSessionProjectionTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike:
  import UserSessionProjectionTest.*

  private val projectionTestKit = ProjectionTestKit(system)

  "The UserSessionProjection" should {
    "process events correctly" in {
      val storageSessionWriter = MockedUserSessionWriter()
      val handler = UserSessionProjectionHandler(system, storageSessionWriter)
      val testUser = UserId("user1")
      val events = StatefulDrivingEvent(Active, SampledLocation(now, testUser, bolognaCampus))
        :: StatefulDrivingEvent(Routing, RoutingStarted(now, testUser, imolaCampus, Driving, cesenaCampus, inTheFuture))
        :: StatefulDrivingEvent(Routing, SampledLocation(now, testUser, forliCampus))
        :: StatefulDrivingEvent(Routing, SampledLocation(now, testUser, cesenaCampus))
        :: Nil
      val sourceEvents = Source(events.zipWithIndex.map(createEnvelope(_, _)))
      val projectId = ProjectionId("user-session", "tracker-0")
      val sourceProvider = TestSourceProvider[Offset, EventEnvelope[RealTimeUserTracker.Event]](
        sourceEvents,
        extractOffset = env => env.offset,
      )
      val projection = TestProjection(projectId, sourceProvider, () => handler)
      val expectedSnapshots = events.collect:
        case StatefulDrivingEvent(state, event: RoutingStarted) => Snapshot(event.user, state, Some(event))
        case StatefulDrivingEvent(state, event: SampledLocation) => Snapshot(event.user, state, Some(event))
      projectionTestKit.run(projection):
        storageSessionWriter.sessions(testUser) shouldBe expectedSnapshots
    }
  }

  private def createEnvelope(event: RealTimeUserTracker.Event, seqNo: Long, timestamp: Long = 0L) =
    EventEnvelope(Offset.sequence(seqNo), "persistenceId", seqNo, event, timestamp)

object UserSessionProjectionTest:
  class MockedUserSessionWriter extends UserSessionWriter[IO, Unit]:
    var sessions: TrieMap[UserId, List[Snapshot]] = TrieMap.empty[UserId, List[Snapshot]]

    override def update(variation: Session.Snapshot): IO[Unit] =
      IO:
        sessions.updateWith(variation.userId)(_.map(_.appended(variation)).orElse(Some(List(variation))))
      *> IO.unit
