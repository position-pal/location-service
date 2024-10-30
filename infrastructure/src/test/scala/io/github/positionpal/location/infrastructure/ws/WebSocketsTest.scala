package io.github.positionpal.location.infrastructure.ws

import io.github.positionpal.location.domain.UserState.Active
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
  private val system = EndOfWorld.startup(8080)(ConfigFactory.load("akka.conf"))

  override def beforeEach(): Unit = system.use(_ => IO.never).unsafeRunAndForget()

  "WebSocket server" when:
    "successfully started" should:
      "be able to handle client requests" in:
        val config = WebSocketTestConfig(baseGroupEndpoint, 5.seconds)
        val test = WebSocketGroupTest(config)
        val scenario = test.Scenario(
          group = GroupId("test-group"),
          clients = test.Client(UserId("test-user-1")) :: test.Client(UserId("test-user-2")) :: Nil,
          events = SampledLocation(TimeUtils.now, UserId("test-user-1"), GeoUtils.cesenaCampusLocation)
            :: SampledLocation(TimeUtils.now, UserId("test-user-2"), GeoUtils.bolognaCampusLocation) :: Nil,
        )
        val expectedEvents = scenario.events.map(_.toUserUpdate)
        val result = test.runTest(scenario)
        whenReady(result): combinedConnectionsResult =>
          combinedConnectionsResult shouldBe true
          Thread.sleep(config.connectionTimeout.toMillis)
          scenario.clients.foreach: client =>
            client.responses should contain allElementsOf expectedEvents

  extension (e: SampledLocation)
    def toUserUpdate: UserUpdate = UserUpdate(e.timestamp, e.user, Some(e.position), Active)

  case class WebSocketTestConfig(baseEndpoint: String, connectionTimeout: FiniteDuration)

  class WebSocketGroupTest(config: WebSocketTestConfig):
    final case class Client(
      id: UserId,
      websocket: WebSocketClient = WebSocketClient(),
      responses: mutable.Set[DrivenEvent] = mutable.Set.empty,
    )
    final case class Scenario[E <: DrivingEvent](group: GroupId, clients: List[Client], events: List[E])

    def runTest[E <: DrivingEvent](scenario: Scenario[E]): Future[Boolean] =
      for
        connectionResults <- connectClients(scenario)
        _ <- sendEvents(scenario)
      yield connectionResults.forall(identity)

    private def connectClients[E <: DrivingEvent](scenario: Scenario[E]): Future[List[Boolean]] =
      Future.sequence:
        scenario.clients.map: client =>
          client.websocket.connect(endpointOf(scenario.group, client.id))(sink(client.responses))

    private def sendEvents[E <: DrivingEvent](scenario: Scenario[E]): Future[List[Unit]] =
      val clientMap = scenario.clients.map(c => c.id -> c.websocket).toMap
      Future.sequence:
        scenario.events.map: event =>
          val message = TextMessage.Strict(Json.encode[DrivingEvent](event).toUtf8String)
          clientMap(event.user).send(message)

    private def endpointOf(group: GroupId, userId: UserId): String =
      s"${config.baseEndpoint}/${group.id}/${userId.id}"

    private def sink(set: mutable.Set[DrivenEvent]): Sink[Message, ?] = Sink.foreach: response =>
      val decoded = Json.decode(response.asTextMessage.getStrictText.getBytes).to[DrivenEvent].valueEither
      decoded.foreach(set.add)
