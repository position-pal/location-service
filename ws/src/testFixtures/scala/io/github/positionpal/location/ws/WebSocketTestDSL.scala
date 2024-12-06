package io.github.positionpal.location.ws

import java.time.Instant

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.Sink
import io.bullet.borer.Json
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent, GPSLocation, SampledLocation}
import io.github.positionpal.location.presentation.ModelCodecs

trait WebSocketTestDSL:
  context: ModelCodecs =>

  import scala.concurrent.ExecutionContext.Implicits.global

  def sample(userId: UserId, location: GPSLocation): SampledLocation = SampledLocation(Instant.now, userId, location)

  case class WebSocketTestConfig(baseEndpoint: String, connectionTimeout: FiniteDuration)

  class WebSocketTest(config: WebSocketTestConfig):
    final case class Client(
        id: UserId,
        websocket: WebSocketClient = WebSocketClient(),
        responses: mutable.Set[DrivenEvent] = mutable.Set.empty,
    )

    final case class Scenario[E <: DrivingEvent](group: GroupId, clients: List[Client], events: List[E] = Nil)

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
      s"${config.baseEndpoint}/${group.value()}/${userId.username()}"

    private def sink(set: mutable.Set[DrivenEvent]): Sink[Message, ?] = Sink.foreach: response =>
      val decoded = Json.decode(response.asTextMessage.getStrictText.getBytes).to[DrivenEvent].valueEither
      decoded.foreach(set.add)
