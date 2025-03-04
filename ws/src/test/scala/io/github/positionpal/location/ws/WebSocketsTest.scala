package io.github.positionpal.location.ws

import scala.concurrent.duration.DurationInt

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import com.typesafe.config.ConfigFactory
import cats.data.Validated.*
import io.github.positionpal.location.application.notifications.NotificationService
import akka.actor.typed.ActorSystem
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import io.github.positionpal.location.tracking.utils.AkkaUtils
import cats.effect.unsafe.implicits.global
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.domain.GeoUtils.*
import org.scalatest.concurrent.Eventually.eventually
import io.github.positionpal.location.presentation.ModelCodecs
import cats.effect.kernel.Resource
import io.github.positionpal.location.domain.UserState.*
import org.scalatest.wordspec.AnyWordSpecLike
import cats.effect.IO
import akka.actor.typed.scaladsl.Behaviors
import io.github.positionpal.location.tracking.ActorBasedRealTimeTracking
import eu.monniot.scala3mock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.{GroupId, UserId}

class WebSocketsTest
    extends AnyWordSpecLike
    with Matchers
    with WebSocketTestDSL
    with ModelCodecs
    with ScalaFutures
    with MockFactory:

  private val maps: MapsService[IO] = mock[MapsService[IO]]
  private val notifier: NotificationService[IO] = mock[NotificationService[IO]]
  private val timeout = Timeout(Span(5, Seconds))
  private val interval = Interval(Span(100, Milliseconds))
  private val testConfig =
    WebSocketTestConfig(baseEndpoint = "ws://localhost:8080/v1/group", connectionTimeout = 5.seconds)
  private val systemResource: Resource[IO, Unit] = for
    actorSystem <- AkkaUtils.startup[IO, Any](ConfigFactory.load("testable-akka-config.conf"))(Behaviors.empty)
    given ActorSystem[Any] = actorSystem
    realTimeTrackingService <- Resource.eval(ActorBasedRealTimeTracking.Service[IO](actorSystem, notifier, maps))
    httpServiceConfig <- Resource.eval(HttpService.Configuration[IO](8080))
    _ <- httpServiceConfig match
      case Valid(config) => HttpService.start[IO](config)(realTimeTrackingService)
      case Invalid(errors) => Resource.eval(IO.raiseError(new Exception(errors.toNonEmptyList.toList.mkString(", "))))
  yield ()

  "WebSocket clients" when:
    "attempting to connect the web socket backend service" should:
      "successfully establish a connection" in:
        systemResource
          .use: _ =>
            IO:
              val test = WebSocketTest(testConfig)
              val scenario = test.Scenario(GroupId.create("guid1"), clients = test.Client(UserId.create("uid1")) :: Nil)
              val result = test.runTest(scenario)
              whenReady(result, timeout, interval): connectionResults =>
                connectionResults shouldBe true
          .unsafeRunSync()

    "successfully connected" should:
      "receive updates from all members of the same group" in:
        systemResource
          .use: _ =>
            IO:
              val luke = UserId.create("luke")
              val eve = UserId.create("eve")
              val group = GroupId.create("astro")
              val test = WebSocketTest(testConfig)
              val scenario = test.Scenario(
                group = group,
                clients = test.Client(luke) :: test.Client(eve) :: Nil,
                events = sample(Scope(luke, group), cesenaCampus.location) ::
                  sample(Scope(eve, group), bolognaCampus.location) :: Nil,
              )
              val expectedEvents = scenario.events.map(_.toUserUpdate)
              val result = test.runTest(scenario)
              whenReady(result, timeout, interval): combinedConnectionsResult =>
                combinedConnectionsResult shouldBe true
                eventually(timeout(Span(30, Seconds)), interval(Span(500, Milliseconds))):
                  scenario.clients.foreach:
                    _.responses should contain allElementsOf expectedEvents
          .unsafeRunSync()

  extension (e: SampledLocation)
    private def toUserUpdate: UserUpdate = UserUpdate(e.timestamp, e.scope, Some(e.position), Active)
