package io.github.positionpal.location.tracking.actors

import java.io.File

import scala.language.postfixOps

import io.github.positionpal.location.domain.EventConversions.{*, given}
import io.github.positionpal.location.domain.TimeUtils.*
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.RoutingMode.*
import io.github.positionpal.location.domain.UserState.*
import cats.effect.IO
import akka.cluster.Cluster
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.domain.Distance.meters
import org.scalatest.concurrent.Eventually
import io.github.positionpal.location.application.notifications.NotificationService
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.{BeforeAndAfterEach, OneInstancePerTest}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import akka.cluster.sharding.typed.scaladsl.ClusterSharding

class RealTimeUserTrackerTest
    extends ScalaTestWithActorTestKit(RealTimeUserTrackerTest.config)
    with AnyWordSpecLike
    with RealTimeUserTrackerVerifierDSL
    with MockFactory
    with OneInstancePerTest
    with BeforeAndAfterEach:

  import RealTimeUserTrackerTest.*

  private val longLastingPatience = Eventually.PatienceConfig(Span(90, Seconds), Span(5, Seconds))

  private val notifier = mock[NotificationService[IO]]
  private val maps = mock[MapsService[IO]]

  when(notifier.sendToGroup).expects(*, *, *).returns(IO.unit).anyNumberOfTimes
  when(maps.distance(_: RoutingMode)(_: GPSLocation, _: GPSLocation))
    .expects(*, *, *)
    .onCall((_, o, d) => if o == d then IO.pure(0.meters) else IO.pure(Double.MaxValue.meters))
    .anyNumberOfTimes

  given ctx: Context[UserState, Session] with
    def notificationService: NotificationService[IO] = notifier
    def mapsService: MapsService[IO] = maps
    def initialStates(ins: List[UserState]): List[Session] =
      ins.map(s => Session.from(testScope, s, sampling(s), tracking(s)))

  override def beforeEach(): Unit =
    super.beforeEach()
    Cluster(system).join(Cluster(system).selfAddress)
    ClusterSharding(system).init(GroupManager())

  "RealTimeUserTracker" when:
    "in inactive or active state" when:
      "receives a new location sample" should:
        "update the last known location" in:
          (Active | Inactive) -- SampledLocation(now, testScope, cesenaCampus) --> Active verifying: (e, s) =>
            s shouldMatch (None, Some(e))

      "receives a routing started event" should:
        "transition to routing mode" in:
          val routingStarted = RoutingStarted(now, testScope, bolognaCampus, Driving, cesenaCampus, inTheFuture)
          (Active | Inactive) -- routingStarted --> Routing verifying: (_, s) =>
            s shouldMatch (Some(routingStarted.toMonitorableTracking), Some(routingStarted: SampledLocation))

    "in routing state" when:
      "reaching the destination" should:
        "transition to active mode" in:
          given Eventually.PatienceConfig = longLastingPatience
          (Routing | Warning) -- SampledLocation(now, testScope, destination) --> Active verifying: (e, s) =>
            s shouldMatch (None, Some(e))

      "receives a routing stopped event" should:
        "transition to active mode" in:
          (Routing | Warning) -- RoutingStopped(now, testScope) --> Active verifying: (_, s) =>
            s shouldMatch (None, Some(defaultContextSample))

    "in SOS state" when:
      "receives a SOS stopped event" should:
        "transition to active mode" in:
          SOS -- SOSAlertStopped(now, testScope) --> Active verifying: (_, s) =>
            s shouldMatch (None, Some(defaultContextSample))

    "in routing or SOS state" when:
      "receives new location samples" should:
        "track the user positions" in:
          val trace = generateTrace
          (Routing | Warning | SOS) -- trace --> (Routing | Routing | SOS) verifying: (_, s) =>
            s shouldMatch (tracking(s.userState, trace.reverse), Some(trace.last))

    "receives an SOS alert triggered event" should:
      "transition to SOS mode" in:
        val sosAlertTriggered = SOSAlertTriggered(now, testScope, cesenaCampus)
        (Active | Inactive | Routing | Warning) -- sosAlertTriggered --> SOS verifying: (_, s) =>
          s shouldMatch (Some(sosAlertTriggered.toTracking), Some(sosAlertTriggered: SampledLocation))

    "inactive for a while" should:
      "transition to inactive mode" in:
        given Eventually.PatienceConfig = longLastingPatience
        val sampledLocation = SampledLocation(now, testScope, cesenaCampus)
        Inactive -- sampledLocation --> Inactive verifying: (_, s) =>
          s.lastSampledLocation shouldBe Some(sampledLocation)

  extension (s: Session)
    infix def shouldMatch(route: Option[Tracking], lastSample: Option[DrivingEvent]): Unit =
      s.tracking shouldBe route
      s.lastSampledLocation shouldBe lastSample

  private def generateTrace: List[SampledLocation] =
    SampledLocation(now, testScope, GPSLocation(44, 12))
      :: SampledLocation(now, testScope, GPSLocation(43, 13))
      :: SampledLocation(now, testScope, GPSLocation(42, 14))
      :: Nil

  private def sampling(state: UserState, sample: SampledLocation = defaultContextSample): Option[SampledLocation] =
    if state != Inactive then Some(sample) else None

  private def tracking(state: UserState, trace: List[SampledLocation] = Nil): Option[Tracking] =
    state match
      case SOS => Some(Tracking(trace))
      case Routing | Warning => Some(Tracking.withMonitoring(Driving, destination, inTheFuture, trace))
      case _ => None

object RealTimeUserTrackerTest:
  import com.typesafe.config.{Config, ConfigFactory}
  import io.github.positionpal.entities.{GroupId, UserId}

  val config: Config = ConfigFactory
    .parseFile(File(ClassLoader.getSystemResource("testable-akka-config.conf").toURI))
    .withFallback(EventSourcedBehaviorTestKit.config)
    .resolve()
  val testUser: UserId = UserId.create("luke")
  val testGroup: GroupId = GroupId.create("astro")
  val testScope: Scope = Scope(testUser, testGroup)
  val defaultContextSample: SampledLocation = SampledLocation(now, testScope, bolognaCampus)
  val destination: GPSLocation = cesenaCampus
