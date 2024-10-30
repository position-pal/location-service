package io.github.positionpal.location.infrastructure.ws

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.infrastructure.*
import io.github.positionpal.location.infrastructure.GeoUtils.*
import io.github.positionpal.location.presentation.ModelCodecs
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

class WebSocketsTest
    extends AnyWordSpecLike
    with BeforeAndAfterEach
    with Matchers
    with WebSocketTestDSL
    with ModelCodecs
    with ScalaFutures:

  private val config = WebSocketTestConfig("ws://localhost:8080/group", 5.seconds)
  private val system = EndOfWorld.startup(8080)(ConfigFactory.load("akka.conf"))

  given Eventually.PatienceConfig = Eventually.PatienceConfig(Span(10, Seconds), Span(2, Seconds))

  override def beforeEach(): Unit = system.use(_ => IO.never).unsafeRunAndForget()

  "WebSocket clients" when:
    "attempting to connect the web socket backend service" should:
      "successfully establish a connection" in:
        val test = WebSocketTest(config)
        val scenario = test.Scenario(group = GroupId("test-group"), clients = test.Client(UserId("uid")) :: Nil)
        val result = test.runTest(scenario)
        whenReady(result): connectionResults =>
          connectionResults shouldBe true

    "successfully connected" should:
      "receive updates from all members of the same group" in:
        val test = WebSocketTest(config)
        val scenario = test.Scenario(
          group = GroupId("test-group"),
          clients = test.Client(UserId("uid1")) :: test.Client(UserId("uid2")) :: Nil,
          events = sample(UserId("uid1"), cesenaCampusLocation) :: sample(UserId("uid2"), bolognaCampusLocation) :: Nil,
        )
        val expectedEvents = scenario.events.map(_.toUserUpdate)
        val result = test.runTest(scenario)
        whenReady(result): combinedConnectionsResult =>
          combinedConnectionsResult shouldBe true
          eventually:
            scenario.clients.foreach: client =>
              client.responses should contain allElementsOf expectedEvents

  extension (e: SampledLocation)
    private def toUserUpdate: UserUpdate = UserUpdate(e.timestamp, e.user, Some(e.position), Active)
