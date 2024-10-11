package io.github.positionpal.location.infrastructure.services

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import io.github.positionpal.location.application.services.UserState
import io.github.positionpal.location.application.services.UserState.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.EventConversions.toMonitorableTracking
import io.github.positionpal.location.domain.RoutingMode.*
import io.github.positionpal.location.infrastructure.GeoUtils.*
import io.github.positionpal.location.infrastructure.TimeUtils.*
import io.github.positionpal.location.infrastructure.services.RealTimeUserTracker.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class RealTimeUserTrackerTest
    extends ScalaTestWithActorTestKit(RealTimeUserTrackerTest.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach:

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Command, DomainEvent, State](system, RealTimeUserTracker("testUser"))
  private val testUser = UserId("user-test")

  override protected def beforeEach(): Unit =
    super.beforeEach()
    eventSourcedTestKit.clear()

  "RealTimeUserTracker" when:
    "initialized" should:
      "have an empty state" in:
        eventSourcedTestKit.getState() shouldMatch (Inactive, None, None)

    "in active state" should:
      "update the last location sample" in:
        val sample = SampledLocation(now, testUser, cesenaCampusLocation)
        eventSourcedTestKit.runCommand(sample).events should contain only sample
        eventSourcedTestKit.getState() shouldMatch (Active, None, Some(sample))

      "transition to routing mode if a routing is started" in:
        val lastSample = SampledLocation(now, testUser, cesenaCampusLocation)
        eventSourcedTestKit.runCommand(lastSample)
        val routingEvent = RoutingStarted(now, testUser, Driving, cesenaCampusLocation, inTheFuture)
        eventSourcedTestKit.runCommand(routingEvent).events should contain only routingEvent
        eventSourcedTestKit.getState() shouldMatch (Routing, Some(routingEvent.toMonitorableTracking), Some(lastSample))

      "transition to sos mode if an sos alert is triggered" in:
        val sosTriggered = SOSAlertTriggered(now, testUser, cesenaCampusLocation)
        eventSourcedTestKit.runCommand(sosTriggered)
        eventSourcedTestKit.getState() shouldMatch (
          SOS,
          Some(Tracking(sosTriggered.user)),
          Some(SampledLocation(sosTriggered.timestamp, sosTriggered.user, sosTriggered.position)),
        )

      "transition to inactive mode after some time not receiving any event" ignore:
        ???

    "in routing state" should:
      "track the user position" ignore:
        val trace = SampledLocation(now, testUser, GPSLocation(44, 12))
          :: SampledLocation(now, testUser, GPSLocation(43, 13))
          :: SampledLocation(now, testUser, GPSLocation(42, 14))
          :: Nil
        eventSourcedTestKit.runCommand(RoutingStarted(now, testUser, Driving, cesenaCampusLocation, inTheFuture))
        trace.reverse.foreach(eventSourcedTestKit.runCommand(_))
        eventSourcedTestKit.getState() shouldMatch (
          Routing,
          Some(Tracking.withMonitoring(testUser, Driving, cesenaCampusLocation, inTheFuture, trace)),
          Some(trace.last),
        )

      "transition to active mode if the routing is stopped" ignore:
        ???

      "transition to active mode if the reaction output is successful" ignore:
        ???

      "send an alert if the reaction output is an alert" ignore:
        ???

      "transition to sos mode if an sos alert is triggered" ignore:
        ???

      "transition to inactive mode after some time not receiving any event" ignore:
        ???

    "in sos state" should:
      "track the user position" ignore:
        ???

      "transition to active mode if the sos alert is stopped" ignore:
        ???

      "transition to inactive mode after some time not receiving any event" ignore:
        ???

    "in inactive state" should:
      "transition to active mode if a new event is received" ignore:
        ???

      "transition to routing mode if a routing is started" ignore:
        ???

      "transition to sos mode if an sos alert is triggered" ignore:
        ???

  extension (s: State)
    infix def shouldMatch(userState: UserState, route: Option[Tracking], lastSample: Option[SampledLocation]): Unit =
      s.userState shouldBe userState
      s.tracking shouldBe route
      s.lastSample shouldBe lastSample

object RealTimeUserTrackerTest:
  val config: Config = ConfigFactory.parseString("""
      akka.actor.provider = "cluster"
      akka.remote.artery.canonical {
        hostname = "127.0.0.1"
        port = 0
      }
      akka.actor {
        serializers {
          borer-json = "io.github.positionpal.location.infrastructure.services.BorerAkkaSerializer"
        }
        serialization-bindings {
          "io.github.positionpal.location.infrastructure.services.Serializable" = borer-json
          "io.github.positionpal.location.domain.DomainEvent" = borer-json
        }
      }
    """).withFallback(EventSourcedBehaviorTestKit.config).resolve()
