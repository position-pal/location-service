package io.github.positionpal.location.infrastructure.ws

import io.github.positionpal.location.presentation.ModelCodecs
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

class WebSocketsTest extends AnyWordSpecLike with BeforeAndAfterEach with Matchers with ModelCodecs with ScalaFutures:

  import akka.http.scaladsl.model.ws.{Message, TextMessage}
  import akka.stream.scaladsl.Sink
  import cats.effect.unsafe.implicits.global
  import cats.effect.IO
  import com.typesafe.config.ConfigFactory
  import io.bullet.borer.Json
  import io.github.positionpal.location.domain.*
  import io.github.positionpal.location.infrastructure.*
  import scala.concurrent.ExecutionContext.Implicits.global

  private val baseGroupEndpoint = "ws://localhost:8080/group"
  private val system1 = EndOfWorld.startup(8080)(ConfigFactory.load("akka.conf"))
  private val system2 = EndOfWorld.startup(8081):
    ConfigFactory.parseString("akka.remote.artery.canonical.port = 2552")
      .withFallback(ConfigFactory.load("akka.conf"))
  private val systems = for
    sys1 <- system1
    sys2 <- system2
  yield (sys1, sys2)

  override def beforeEach(): Unit = systems.use(_ => IO.never).unsafeRunAndForget()

  "WebSocket server" when:
    "successfully started" should:
      "be able to handle client requests" in:
        val config = WebSocketTestConfig(baseGroupEndpoint)
        val test = WebSocketGroupTest(config)
        val scenario = test.Scenario(
          group = GroupId("test-group"),
          clients = test.Client(UserId("test-user-1")) :: test.Client(UserId("test-user-2")) :: Nil,
          events = SampledLocation(TimeUtils.now, UserId("test-user-1"), GeoUtils.cesenaCampusLocation)
            :: SampledLocation(TimeUtils.now, UserId("test-user-2"), GeoUtils.bolognaCampusLocation) :: Nil,
        )
        val result = test.runTest(scenario)
        whenReady(result): combinedResult =>
          combinedResult shouldBe true
          Thread.sleep(config.connectionTimeout.toMillis)
          test.verifyResults(scenario)

      "again" in:  
        val group = GroupId("test-group-0")
        val user1 = UserId("test-user-1")
        val user2 = UserId("test-user-2")
        val updatesUser1 = mutable.Set.empty[DrivenEvent]
        val updatesUser2 = mutable.Set.empty[DrivenEvent]
        val eventUser1: DrivingEvent = SampledLocation(TimeUtils.now, user1, GeoUtils.cesenaCampusLocation)
        val eventUser2: DrivingEvent = SampledLocation(TimeUtils.now, user2, GeoUtils.bolognaCampusLocation)
        val client1 = WebSocketClient()
        val client2 = WebSocketClient()
        val res = for
          connectionResultClient1 <- client1.connect(s"$baseGroupEndpoint/${group.id}/${user1.id}")(sink(updatesUser1))
          connectionResultClient2 <- client2.connect(s"$baseGroupEndpoint/${group.id}/${user2.id}")(sink(updatesUser2))
          _ <- client1.send(TextMessage.Strict(Json.encode(eventUser1).toUtf8String))
          _ <- client2.send(TextMessage.Strict(Json.encode(eventUser2).toUtf8String))
        yield connectionResultClient1 && connectionResultClient2

        whenReady(res): combinedResult =>
          combinedResult shouldBe true

          Thread.sleep(5_000)

          println(">>>>>>>>> RESULTS")
          println(updatesUser1)
          println(updatesUser2)

  case class WebSocketTestConfig(baseEndpoint: String, connectionTimeout: FiniteDuration = 5.seconds)

  class WebSocketGroupTest(config: WebSocketTestConfig):
    final case class Client(
      id: UserId,
      websocket: WebSocketClient = WebSocketClient(),
      responses: mutable.Set[DrivenEvent] = mutable.Set.empty,
    )
    final case class Scenario(group: GroupId, clients: List[Client], events: List[DrivingEvent])

    def runTest(scenario: Scenario): Future[Boolean] =
      for
        connectionResults <- connectClients(scenario)
        _ <- sendEvents(scenario)
      yield connectionResults.forall(identity)

    private def connectClients(scenario: Scenario): Future[List[Boolean]] =
      Future.sequence:
        scenario.clients.map: client =>
          client.websocket.connect(endpointOf(scenario.group, client.id))(sink(client.responses))

    private def sendEvents(scenario: Scenario): Future[List[Unit]] =
      val clientMap = scenario.clients.map(c => c.id -> c.websocket).toMap
      Future.sequence:
        scenario.events.map: event =>
          val message = TextMessage.Strict(Json.encode(event).toUtf8String)
          clientMap(event.user).send(message)

    private def endpointOf(group: GroupId, userId: UserId): String =
      s"${config.baseEndpoint}/${group.id}/${userId.id}"

    def verifyResults(scenario: Scenario): Unit =
      scenario.clients.foreach: client =>
        println(s"Updates for user ${client.id}:")
        println(client.responses)

  def sink(set: mutable.Set[DrivenEvent]): Sink[Message, ?] = Sink.foreach: response =>
    val decoded = Json.decode(response.asTextMessage.getStrictText.getBytes).to[DrivenEvent].valueEither
    decoded.foreach(set.add)
