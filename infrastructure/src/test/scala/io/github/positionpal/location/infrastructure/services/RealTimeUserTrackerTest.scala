package io.github.positionpal.location.infrastructure.services

import scala.language.postfixOps

import akka.actor.testkit.typed.scaladsl.{ActorTestKitBase, ScalaTestWithActorTestKit}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import io.github.positionpal.location.application.services.UserState
import io.github.positionpal.location.application.services.UserState.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.EventConversions.*
import io.github.positionpal.location.domain.RoutingMode.*
import io.github.positionpal.location.infrastructure.GeoUtils.*
import io.github.positionpal.location.infrastructure.TimeUtils.*
import io.github.positionpal.location.infrastructure.services.RealTimeUserTracker.*
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, time}

class RealTimeUserTrackerTest
    extends ScalaTestWithActorTestKit(RealTimeUserTrackerTest.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with ActorTestVerifierDSL:

  private val testUser = UserId("user-test")
  private val routingStartedEvent = RoutingStarted(now, testUser, Driving, cesenaCampusLocation, inTheFuture)
  private val sampledLocationEvent = SampledLocation(now, testUser, cesenaCampusLocation)
  private val sosAlertTriggered = SOSAlertTriggered(now, testUser, cesenaCampusLocation)

  given Context[UserState, State] = ins =>
    ins.map: s =>
      State(
        s,
        s match
          case SOS => Some(sosAlertTriggered.toTracking)
          case Routing => Some(routingStartedEvent.toMonitorableTracking)
          case _ => None,
        None,
      )

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

      "receives an SOS alert triggered event" should:
        "transition to SOS mode" in:
          (Active | Inactive | Routing) -- sosAlertTriggered --> SOS verifying: (_, s) =>
            s shouldMatch (Some(sosAlertTriggered.toTracking), Some(sosAlertTriggered.toSampledLocation))

    "in routing or SOS state" when:
      "receives new location samples" should:
        "track the user positions" ignore:
          val trace = generateTrace
          (Routing | SOS) -- trace --> (Routing | SOS) verifying: (_, s) =>
            s shouldMatch (Some(fromTrace(s.userState, trace)), Some(trace.last))

    "in routing state" when:
      "reaching the destination" should:
        "transition to active mode" in:
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

  /*
      "transition to inactive mode after some time not receiving any event" ignore:
        ???

    "in routing state" should:

      "transition to inactive mode after some time not receiving any event" ignore:
        ???

    "in sos state" should:

      "transition to inactive mode after some time not receiving any event" ignore:
        ???
   */

  extension (s: State)
    infix def shouldMatch(route: Option[Tracking], lastSample: Option[DomainEvent]): Unit =
      s.tracking shouldBe route
      s.lastSample shouldBe lastSample

  private def generateTrace: List[SampledLocation] =
    SampledLocation(now, testUser, GPSLocation(44, 12))
      :: SampledLocation(now, testUser, GPSLocation(43, 13))
      :: SampledLocation(now, testUser, GPSLocation(42, 14))
      :: Nil

  private def fromTrace(userState: UserState, trace: List[SampledLocation]): Tracking | MonitorableTracking =
    if userState == SOS then Tracking(testUser, trace)
    else Tracking.withMonitoring(testUser, Driving, cesenaCampusLocation, inTheFuture, trace)

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

trait SystemVerifier[S, E, X](val ins: List[S], val events: List[E]):
  infix def -->(outs: List[S])(using ctx: Context[S, X]): Verification[E, X]

trait Verification[E, X]:
  infix def verifying(verifyLast: (E, X) => Unit): Unit

trait Context[S, X]:
  def initialStates(ins: List[S]): List[X]

trait ActorTestVerifierDSL:
  context: ActorTestKitBase & Matchers =>

  given Conversion[UserState, List[UserState]] = _ :: Nil
  given Conversion[DomainEvent, List[DomainEvent]] = _ :: Nil
  extension (u: UserState) infix def |(other: UserState): List[UserState] = u :: other :: Nil
  extension (us: List[UserState]) infix def |(other: UserState): List[UserState] = us :+ other

  extension (xs: List[UserState])
    infix def --(events: List[DomainEvent]): SystemVerifier[UserState, DomainEvent, RealTimeUserTracker.State] =
      RealTimeUserTrackerVerifier(xs, events)

  class RealTimeUserTrackerVerifier(ins: List[UserState], events: List[DomainEvent])
      extends SystemVerifier[UserState, DomainEvent, State](ins, events):
    override infix def -->(outs: List[UserState])(using
        ctx: Context[UserState, State],
    ): Verification[DomainEvent, State] =
      (verifyLast: (Event, State) => Unit) =>
        val testKit = EventSourcedBehaviorTestKit[Command, DomainEvent, State](system, RealTimeUserTracker("testUser"))
        ctx.initialStates(ins).zipWithIndex.foreach: (state, idx) =>
          testKit.initialize(state)
          println(s">>> Initial state: ${testKit.getState()}")
          events.foreach: ev =>
            testKit.runCommand(ev) // should contain only ev
          eventually(Timeout(Span(20, Seconds)), Interval(Span(5, Seconds))):
            val currentState = testKit.getState()
            println(s">>> Current state: $currentState")
            currentState.userState shouldBe outs(if outs.size == 1 then 0 else idx)
            verifyLast(events.last, currentState)
