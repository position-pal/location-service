package io.github.positionpal.location.infrastructure.services.actors

import akka.actor.ExtendedActorSystem
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorRefResolver}
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodec
import io.bullet.borer.{Codec, Decoder, Encoder}
import io.github.positionpal.location.domain.{DrivenEvent, DrivingEvent}
import io.github.positionpal.location.infrastructure.ws.WebSockets
import io.github.positionpal.location.presentation.*

/** Custom Akka serializer for the actor entities. */
class BorerAkkaSerializer(system: ExtendedActorSystem) extends BorerCborAkkaSerializer with ModelCodecs:
  private val actorRefResolver = ActorRefResolver(system.toTyped)

  override def identifier: Int = 19923

  given actorRefCodec[T]: Codec[ActorRef[T]] =
    Codec.bimap[String, ActorRef[T]](
      ref => actorRefResolver.toSerializationFormat(ref),
      str => actorRefResolver.resolveActorRef(str),
    )
  given observableSession: Codec[RealTimeUserTracker.ObservableSession] =
    deriveCodec[RealTimeUserTracker.ObservableSession]
  given ignoreCoded: Codec[RealTimeUserTracker.Ignore.type] = deriveCodec[RealTimeUserTracker.Ignore.type]
  given aliveCheckCodec: Codec[RealTimeUserTracker.AliveCheck.type] = deriveCodec[RealTimeUserTracker.AliveCheck.type]
  given wireCodec: Codec[RealTimeUserTracker.Wire] = deriveCodec[RealTimeUserTracker.Wire]
  given unWireCodec: Codec[RealTimeUserTracker.UnWire] = deriveCodec[RealTimeUserTracker.UnWire]
  given replyCodec: Codec[WebSockets.Reply] = deriveCodec[WebSockets.Reply]

  register[RealTimeUserTracker.AliveCheck.type]()
  register[RealTimeUserTracker.Ignore.type]()
  register[DrivingEvent]()
  register[DrivenEvent]()
  register[RealTimeUserTracker.ObservableSession]()
  register[RealTimeUserTracker.Wire]()
  register[RealTimeUserTracker.UnWire]()
  register[WebSockets.Reply]()
