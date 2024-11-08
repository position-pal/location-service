package io.github.positionpal.location.infrastructure.services.actors

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
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

class RealTimeUserTrackerTest
    extends ScalaTestWithActorTestKit(RealTimeUserTrackerTest.config)
    with AnyWordSpecLike
    with RealTimeUserTrackerVerifierDSL:

  private val testUser = UserId("user-test")
  private val defaultContextSample = SampledLocation(now, testUser, bolognaCampus)
  private val destination = cesenaCampus
  private val longLastingPatience = Eventually.PatienceConfig(Span(30, Seconds), Span(5, Seconds))

  given Context[UserState, ObservableSession] = ins =>
    ins.map(s => ObservableSession(Session.from(testUser, s, sampling(s), tracking(s)), Set.empty))

  "RealTimeUserTracker" when:
    "in inactive or active state" when:
      "receives a new location sample" should:
        "update the last known location" in:
          (Active | Inactive) -- SampledLocation(now, testUser, cesenaCampus) --> Active verifying: (e, s) =>
            s shouldMatch (None, Some(e))

      "receives a routing started event" should:
        "transition to routing mode" in:
          val routingStarted = RoutingStarted(now, testUser, bolognaCampus, Driving, cesenaCampus, inTheFuture)
          (Active | Inactive) -- routingStarted --> Routing verifying: (_, s) =>
            s shouldMatch (Some(routingStarted.toMonitorableTracking), Some(routingStarted: SampledLocation))

    "in routing state" when:
      "reaching the destination" should:
        "transition to active mode" in:
          given Eventually.PatienceConfig = longLastingPatience
          Routing -- SampledLocation(now, testUser, destination) --> Active verifying: (e, s) =>
            s shouldMatch (None, Some(e))

      "receives a routing stopped event" should:
        "transition to active mode" in:
          Routing -- RoutingStopped(now, testUser) --> Active verifying: (_, s) =>
            s shouldMatch (None, Some(defaultContextSample))

    "in SOS state" when:
      "receives a SOS stopped event" should:
        "transition to active mode" in:
          SOS -- SOSAlertStopped(now, testUser) --> Active verifying: (_, s) =>
            s shouldMatch (None, Some(defaultContextSample))

    "in routing or SOS state" when:
      "receives new location samples" should:
        "track the user positions" in:
          val trace = generateTrace
          (Routing | SOS) -- trace --> (Routing | SOS) verifying: (_, s) =>
            s shouldMatch (tracking(s.session.userState, trace.reverse), Some(trace.last))

    "receives an SOS alert triggered event" should:
      "transition to SOS mode" in:
        val sosAlertTriggered = SOSAlertTriggered(now, testUser, cesenaCampus)
        (Active | Inactive | Routing) -- sosAlertTriggered --> SOS verifying: (_, s) =>
          s shouldMatch (Some(Tracking()), Some(sosAlertTriggered: SampledLocation))

    "inactive for a while" should:
      "transition to inactive mode" in:
        given Eventually.PatienceConfig = longLastingPatience
        val sampledLocation = SampledLocation(now, testUser, cesenaCampus)
        (Active | Routing | SOS) -- sampledLocation --> Inactive verifying: (_, s) =>
          s.session.lastSampledLocation shouldBe Some(sampledLocation)

  extension (s: ObservableSession)
    infix def shouldMatch(route: Option[Tracking], lastSample: Option[DrivingEvent]): Unit =
      s.session.tracking shouldBe route
      s.session.lastSampledLocation shouldBe lastSample

  private def generateTrace: List[SampledLocation] =
    SampledLocation(now, testUser, GPSLocation(44, 12))
      :: SampledLocation(now, testUser, GPSLocation(43, 13))
      :: SampledLocation(now, testUser, GPSLocation(42, 14))
      :: Nil

  private def sampling(state: UserState, sample: SampledLocation = defaultContextSample): Option[SampledLocation] =
    if state != Inactive then Some(sample) else None

  private def tracking(state: UserState, trace: List[SampledLocation] = Nil): Option[Tracking] =
    state match
      case SOS => Some(Tracking(trace))
      case Routing => Some(Tracking.withMonitoring(Driving, cesenaCampus, inTheFuture, trace))
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
