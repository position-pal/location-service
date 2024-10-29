package io.github.positionpal.location.infrastructure

object GeoUtils:
  import io.github.positionpal.location.domain.GPSLocation

  val cesenaCampusLocation: GPSLocation = GPSLocation(44.1476299926484, 12.2357184467018)
  val bolognaCampusLocation: GPSLocation = GPSLocation(44.487912, 11.32885)
end GeoUtils

object TimeUtils:
  import java.util.Date

  def now: Date = Date()
  def inTheFuture: Date = Date.from(now.toInstant.plusSeconds(90))
  def inThePast: Date = Date.from(now.toInstant.minusSeconds(90))
end TimeUtils

class WebSocketClient:

  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
  import akka.http.scaladsl.model.Uri
  import akka.stream.scaladsl.{Source, Sink, Flow}
  import akka.stream.Materializer
  import akka.actor.ActorSystem
  import akka.stream.scaladsl.Keep
  import akka.http.scaladsl.model.ws.WebSocketUpgradeResponse
  import akka.stream.OverflowStrategy
  import akka.stream.scaladsl.SourceQueueWithComplete
  import scala.concurrent.{ExecutionContext, Future}

  given actorSystem: ActorSystem = ActorSystem("WebsocketClient")
  given materializer: Materializer = Materializer(actorSystem)
  given ExecutionContext = actorSystem.dispatcher

  private var webSocketFlow: Option[Flow[Message, Message, Future[WebSocketUpgradeResponse]]] = None
  private var messageSink: Option[SourceQueueWithComplete[Message]] = None

  def connect(url: String)(incoming: Sink[Message, ?]): Future[Boolean] =
    val flow = Http().webSocketClientFlow(WebSocketRequest(url))
    webSocketFlow = Some(flow)
    val outgoing = Source.queue[Message](bufferSize = 100, overflowStrategy = OverflowStrategy.dropHead)
    val (sourceQueue, upgradeResponse) = outgoing
      .viaMat(flow)(Keep.both)
      .toMat(incoming)(Keep.left)
      .run()
    messageSink = Some(sourceQueue)
    upgradeResponse.map: upgrade =>
      upgrade.response.status.isSuccess

  def send(message: Message): Future[Unit] =
    messageSink match
      case Some(sink) => Future(sink.offer(message))
      case None => Future.failed(IllegalStateException("WebSocket connection not established. Call `connect()` first."))

  def shutdown(): Unit =
    Http().shutdownAllConnectionPools()
    actorSystem.terminate()

end WebSocketClient
