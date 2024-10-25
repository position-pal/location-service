package io.github.positionpal.location.infrastructure.services

import scala.language.postfixOps

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.EventConversions.{*, given}
import io.github.positionpal.location.domain.RoutingMode.*
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.infrastructure.GeoUtils.*
import io.github.positionpal.location.infrastructure.TimeUtils.*
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

class RealTimeUserTrackerTest
    extends ScalaTestWithActorTestKit(RealTimeUserTrackerTest.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with RealTimeUserTrackerVerifierDSL:

  private val testUser = UserId("user-test")
  private val routingStartedEvent = RoutingStarted(now, testUser, Driving, cesenaCampusLocation, inTheFuture)
  private val sampledLocationEvent = SampledLocation(now, testUser, cesenaCampusLocation)
  private val sosAlertTriggered = SOSAlertTriggered(now, testUser, cesenaCampusLocation)
  private val longLastingPatience = Eventually.PatienceConfig(Span(60, Seconds), Span(5, Seconds))

  given Context[UserState, State] = ins => ins.map(s => State(s, tracking(s), None))

  "RealTimeUserTracker" when:
    "in inactive or active state" when:
      "receives a new location sample" should:
        "update the last known location" in:
          (Active | Inactive) -- sampledLocationEvent --> Active verifying: (e, s) =>
            s shouldMatch (None, Some(e))

      "receives a routing started event" should:
        "transition to routing mode" in:
          (Active | Inactive) -- routingStartedEvent --> Routing verifying: (_, s) =>
            s shouldMatch (Some(routingStartedEvent.toMonitorableTracking), None)

    "in routing state" when:
      "reaching the destination" should:
        "transition to active mode" in:
          given Eventually.PatienceConfig = longLastingPatience
          Routing -- SampledLocation(now, testUser, cesenaCampusLocation) --> Active verifying: (e, s) =>
            s shouldMatch (None, Some(e))

      "receives a routing stopped event" should:
        "transition to active mode" in:
          Routing -- RoutingStopped(now, testUser) --> Active verifying: (_, s) =>
            s shouldMatch (None, None)

    "in SOS state" when:
      "receives a SOS stopped event" should:
        "transition to active mode" in:
          SOS -- SOSAlertStopped(now, testUser) --> Active verifying: (_, s) =>
            s shouldMatch (None, None)

    "in routing or SOS state" when:
      "receives new location samples" should:
        "track the user positions" ignore:
          val trace = generateTrace
          (Routing | SOS) -- trace --> (Routing | SOS) verifying: (_, s) =>
            s shouldMatch (tracking(s.userState, trace), Some(trace.last))

    "receives an SOS alert triggered event" should:
      "transition to SOS mode" in:
        (Active | Inactive | Routing) -- sosAlertTriggered --> SOS verifying: (_, s) =>
          s shouldMatch (Some(sosAlertTriggered.toTracking), Some(sosAlertTriggered: SampledLocation))

    "inactive for a while" should:
      "transition to inactive mode" in:
        given Eventually.PatienceConfig = longLastingPatience
        (Active | Routing | SOS) -- sampledLocationEvent --> Inactive verifying: (_, s) =>
          s.lastSample shouldBe Some(sampledLocationEvent)

  extension (s: State)
    infix def shouldMatch(route: Option[Tracking], lastSample: Option[DrivingEvent]): Unit =
      s.tracking shouldBe route
      s.lastSample shouldBe lastSample

  private def generateTrace: List[SampledLocation] =
    SampledLocation(now, testUser, GPSLocation(44, 12))
      :: SampledLocation(now, testUser, GPSLocation(43, 13))
      :: SampledLocation(now, testUser, GPSLocation(42, 14))
      :: Nil

  private def tracking(state: UserState, trace: List[SampledLocation] = Nil): Option[Tracking | MonitorableTracking] =
    state match
      case SOS => Some(Tracking(testUser, trace))
      case Routing => Some(Tracking.withMonitoring(testUser, Driving, cesenaCampusLocation, inTheFuture, trace))
      case _ => None

object RealTimeUserTrackerTest:
  val config: Config = ConfigFactory.parseString("""
      akka.actor.provider = "cluster"
      akka.remote.artery.canonical {
        hostname = "127.0.0.1"
        port = 0
      }
      akka.actor {
        serializers {
          borer-json = "io.github.positionpal.location.infrastructure.services.actors.BorerAkkaSerializer"
        }
        serialization-bindings {
          "io.github.positionpal.location.infrastructure.services.actors.AkkaSerializable" = borer-json
          "io.github.positionpal.location.domain.DrivingEvent" = borer-json
        }
      }
    """).withFallback(EventSourcedBehaviorTestKit.config).resolve()
