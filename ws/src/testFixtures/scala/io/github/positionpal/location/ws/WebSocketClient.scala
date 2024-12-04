package io.github.positionpal.location.ws

class WebSocketClient:

  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.model.Uri
  import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
  import akka.stream.scaladsl.*
  import akka.stream.{Materializer, OverflowStrategy}

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
    val (sourceQueue, upgradeResponse) = outgoing.viaMat(flow)(Keep.both).toMat(incoming)(Keep.left).run()
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
