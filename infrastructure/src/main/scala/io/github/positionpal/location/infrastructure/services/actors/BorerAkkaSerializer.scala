package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.ExtendedActorSystem
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorRefResolver}
import io.bullet.borer.{Codec, Decoder, Encoder}
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodec
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent}
import io.github.positionpal.location.infrastructure.ws.WebSockets
import io.github.positionpal.location.presentation.*

/** Custom Akka serializer for the RealTimeUserTracker actor. */
class BorerAkkaSerializer(system: ExtendedActorSystem) extends BorerCborAkkaSerializer with ModelCodecs:
  private val actorRefResolver = ActorRefResolver(system.toTyped)

  override def identifier: Int = 19923

  given actorRefCodec[T]: Codec[ActorRef[T]] =
    Codec.bimap[String, ActorRef[T]](
      ref => actorRefResolver.toSerializationFormat(ref),
      str => actorRefResolver.resolveActorRef(str),
    )
  given stateCodec: Codec[RealTimeUserTracker.State] = deriveCodec[RealTimeUserTracker.State]
  given ignoreCoded: Codec[RealTimeUserTracker.Ignore.type] = deriveCodec[RealTimeUserTracker.Ignore.type]
  given alignCheckCodec: Codec[RealTimeUserTracker.AliveCheck.type] = deriveCodec[RealTimeUserTracker.AliveCheck.type]
  given reactCodec: Codec[RealTimeUserTracker.React] =
    Codec[RealTimeUserTracker.React](
      Encoder[RealTimeUserTracker.React]: (writer, react) =>
        writer.writeArrayOpen(2).write(react.e).write(react.replyTo).writeArrayClose(),
      Decoder[RealTimeUserTracker.React]: reader =>
        val unbounded = reader.readArrayOpen(2)
        reader.readArrayClose(
          unbounded, RealTimeUserTracker.React(reader.read[DrivingEvent]())(reader.read[ActorRef[GroupManager.Command]]())
        )
    )
  given wireCodec: Codec[GroupManager.Wire] = deriveCodec[GroupManager.Wire]
  given unwireCodec: Codec[GroupManager.UnWire] = deriveCodec[GroupManager.UnWire]
  given replyCodec: Codec[WebSockets.Reply] = deriveCodec[WebSockets.Reply]
  given eventCodec: Codec[GroupManager.Event] = deriveCodec[GroupManager.Event]
  
  register[RealTimeUserTracker.AliveCheck.type]()
  register[RealTimeUserTracker.Ignore.type]()
  register[DrivingEvent]()
  register[DrivenEvent]()
  register[RealTimeUserTracker.State]()
  register[GroupManager.Wire]()
  register[GroupManager.UnWire]()
  register[GroupManager.Event]()
  register[WebSockets.Reply]()
  register[RealTimeUserTracker.React]()
