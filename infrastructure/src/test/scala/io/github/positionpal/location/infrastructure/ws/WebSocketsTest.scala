package io.github.positionpal.location.infrastructure.ws

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.infrastructure.*
import io.github.positionpal.location.infrastructure.GeoUtils.*
import io.github.positionpal.location.presentation.ModelCodecs
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

class WebSocketsTest extends AnyWordSpecLike with Matchers with WebSocketTestDSL with ModelCodecs with ScalaFutures:

  private val config = WebSocketTestConfig(baseEndpoint = "ws://localhost:8080/group", connectionTimeout = 5.seconds)
  private val systemResource = EndOfWorld.startup(8080)(ConfigFactory.load("akka.conf"))

  given patience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Milliseconds))

  "WebSocket clients" when:
    "attempting to connect the web socket backend service" should:
      "successfully establish a connection" in:
        systemResource.use: _ =>
          IO:
            val test = WebSocketTest(config)
            val scenario = test.Scenario(GroupId.create("guid1"), clients = test.Client(UserId.create("uid1")) :: Nil)
            val result = test.runTest(scenario)
            whenReady(result): connectionResults =>
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
            whenReady(result): combinedConnectionsResult =>
              combinedConnectionsResult shouldBe true
              eventually(timeout(Span(30, Seconds)), interval(Span(500, Milliseconds))):
                scenario.clients.foreach:
                  _.responses should contain allElementsOf expectedEvents
        .unsafeRunSync()

  extension (e: SampledLocation)
    private def toUserUpdate: UserUpdate = UserUpdate(e.timestamp, e.user, Some(e.position), Active)
