package io.github.positionpal.location.tracking.projections

import scala.collection.concurrent.TrieMap

import akka.stream.scaladsl.Source
import io.github.positionpal.location.domain.EventConversions.given
import io.github.positionpal.location.domain.TimeUtils.*
import io.github.positionpal.location.tracking.actors.RealTimeUserTracker
import akka.projection.ProjectionId
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.tracking.actors.RealTimeUserTracker.StatefulDrivingEvent
import io.github.positionpal.location.domain.RoutingMode.Driving
import io.github.positionpal.location.domain.UserState.*
import org.scalatest.wordspec.AnyWordSpecLike
import cats.effect.IO
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import io.github.positionpal.location.domain.Session.Snapshot
import akka.projection.testkit.scaladsl.{ProjectionTestKit, TestProjection, TestSourceProvider}
import akka.persistence.query.Offset
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.{GroupId, UserId}
import akka.projection.eventsourced.EventEnvelope
import io.github.positionpal.location.application.sessions.UserSessionsWriter

class UserSessionProjectionTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike:
  import UserSessionProjectionTest.*

  private val projectionTestKit = ProjectionTestKit(system)

  "The UserSessionProjection" should {
    "process events correctly" in {
      val storageSessionWriter = MockedUserSessionsWriter()
      val handler = UserSessionProjectionHandler(system, storageSessionWriter)
      val scope = Scope(UserId.create("luke"), GroupId.create("astro"))
      val events = StatefulDrivingEvent(Active, SampledLocation(now, scope, bolognaCampus.location))
        :: StatefulDrivingEvent(
          Routing,
          RoutingStarted(now, scope, imolaCampus.location, Driving, cesenaCampus, inTheFuture),
        )
        :: StatefulDrivingEvent(Routing, SampledLocation(now, scope, forliCampus.location))
        :: StatefulDrivingEvent(Routing, SampledLocation(now, scope, cesenaCampus.location))
        :: Nil
      val sourceEvents = Source(events.zipWithIndex.map(createEnvelope(_, _)))
      val projectId = ProjectionId("user-session", "tracker-0")
      val sourceProvider = TestSourceProvider[Offset, EventEnvelope[RealTimeUserTracker.Event]](
        sourceEvents,
        extractOffset = env => env.offset,
      )
      val projection = TestProjection(projectId, sourceProvider, () => handler)
      val expectedSnapshots = events.collect:
        case StatefulDrivingEvent(state, event: RoutingStarted) => Snapshot(event.scope, state, Some(event))
        case StatefulDrivingEvent(state, event: SampledLocation) => Snapshot(event.scope, state, Some(event))
      projectionTestKit.run(projection):
        storageSessionWriter.sessions(scope) shouldBe expectedSnapshots
    }
  }

  private def createEnvelope(event: RealTimeUserTracker.Event, seqNo: Long, timestamp: Long = 0L) =
    EventEnvelope(Offset.sequence(seqNo), "persistenceId", seqNo, event, timestamp)

object UserSessionProjectionTest:
  class MockedUserSessionsWriter extends UserSessionsWriter[IO, Unit]:
    var sessions: TrieMap[Scope, List[Snapshot]] = TrieMap.empty[Scope, List[Snapshot]]
    override def update(variation: Session.Snapshot): IO[Unit] = IO:
      sessions.updateWith(variation.scope)(_.map(_.appended(variation)).orElse(Some(List(variation))))
    *> IO.unit
