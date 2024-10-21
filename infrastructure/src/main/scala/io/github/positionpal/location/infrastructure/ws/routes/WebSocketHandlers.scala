package io.github.positionpal.location.infrastructure.ws.routes

import io.github.positionpal.location.infrastructure.ws.Protocol.*

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.OverflowStrategy
import akka.cluster.sharding.typed.ShardingEnvelope

/** Object that contains the Flow handlers for websocket connections. */
object WebSocketHandlers:

  /** Default websocket handler.
    * @param incomingActorRef The incomingMessage flow.
    * @return The [[Flow]] object used for handling the messages on the websocket routes
    */
  def websocketHandler(
    groupId: String,
    sessionRef: ActorRef[OutgoingEvent],
    incomingActorRef: ActorRef[ShardingEnvelope[IncomingEvent]]
  ): Flow[Message, Message, WebSocketHandler] =

    val incomingMessage: Sink[Message, NotUsed] =
      Flow[Message].map:
        case TextMessage.Strict(text) =>
          println(s"Received message: $text")
          ShardingEnvelope(groupId, Msg(text))
      .to:
        ActorSink.actorRef(
          incomingActorRef,
          onCompleteMessage = ShardingEnvelope(groupId, StreamCompletedSuccessfully),
          onFailureMessage = ex => ShardingEnvelope(groupId, StreamCompletedWithException(ex)),
        )

    val outgoingMessage: Source[Message, ActorRef[OutgoingEvent]] =
      ActorSource.actorRef[OutgoingEvent](
        completionMatcher = { case Completed => () },
        failureMatcher = { case Failure(ex) => ex },
        bufferSize = 100,
        overflowStrategy = OverflowStrategy.fail,
      ).map: (protocolMessage: OutgoingEvent) =>
        protocolMessage match
          case WsMsg(_, content) => TextMessage.Strict(content)

    Flow.fromSinkAndSourceMat(incomingMessage, outgoingMessage):
      case (_, outgoingActorRef) =>
        incomingActorRef ! ShardingEnvelope(groupId, Connected(sessionRef))
        sessionRef ! NewConnection(groupId, outgoingActorRef)
        WebSocketHandler(outgoingActorRef.narrow[OutgoingEvent])
