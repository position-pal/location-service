package io.github.positionpal.location.ws

import scala.concurrent.duration.DurationInt

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import io.github.positionpal.location.presentation.ModelCodecs
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

class WebSocketsTest extends AnyWordSpecLike with Matchers with WebSocketTestDSL with ModelCodecs with ScalaFutures:

  private val timeout = Timeout(Span(5, Seconds))
  private val interval = Interval(Span(100, Milliseconds))
  private val config = WebSocketTestConfig(baseEndpoint = "ws://localhost:8080/group", connectionTimeout = 5.seconds)
  private val systemResource: Resource[IO, Unit] = for
    actorSystem <- AkkaUtils.startup[IO, Any](ConfigFactory.load("akka.conf"))(Behaviors.empty)
    given ActorSystem[Any] = actorSystem
    realTimeTrackingService <- Resource.eval(ActorBasedRealTimeTracking.Service[IO](actorSystem))
    _ <- HttpService.start[IO](8080)(realTimeTrackingService)
  yield ()

  "WebSocket clients" when:
    "attempting to connect the web socket backend service" should:
      "successfully establish a connection" in:
        systemResource.use: _ =>
          IO:
            val test = WebSocketTest(config)
            val scenario = test.Scenario(GroupId.create("guid1"), clients = test.Client(UserId.create("uid1")) :: Nil)
            val result = test.runTest(scenario)
            whenReady(result, timeout, interval): connectionResults =>
              connectionResults shouldBe true
        .unsafeRunSync()

    "successfully connected" should:
      "receive updates from all members of the same group" in:
        systemResource.use: _ =>
          IO:
            val user1 = UserId.create("u1")
            val user2 = UserId.create("u2")
            val test = WebSocketTest(config)
            val scenario = test.Scenario(
              group = GroupId.create("test-group"),
              clients = test.Client(user1) :: test.Client(user2) :: Nil,
              events = sample(user1, cesenaCampus) :: sample(user2, bolognaCampus) :: Nil,
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
    private def toUserUpdate: UserUpdate = UserUpdate(e.timestamp, e.user, Some(e.position), Active)
