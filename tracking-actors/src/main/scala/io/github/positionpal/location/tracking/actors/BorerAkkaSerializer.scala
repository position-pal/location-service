package io.github.positionpal.location.tracking.actors

import akka.actor.typed.{ActorRef, ActorRefResolver}
import akka.actor.ExtendedActorSystem
import io.bullet.borer.{Codec, Decoder, Encoder}
import io.github.positionpal.location.domain.{ClientDrivingEvent, DrivenEvent, Session}
import io.github.positionpal.location.presentation.*
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodec
import akka.actor.typed.scaladsl.adapter.*

/** Custom Akka serializer for the actor entities, where actor events and commands are registered into
  * [[BorerCborAkkaSerializer]] for serialization and deserialization.
  * This class should to be registered in the `akka.actor.serializers` entry in the akka serialization configuration.
  */
class BorerAkkaSerializer(system: ExtendedActorSystem) extends BorerCborAkkaSerializer with ModelCodecs:
  override def identifier: Int = 19923

  private val actorRefResolver = ActorRefResolver(system.toTyped)

  given actorRefCodec[T]: Codec[ActorRef[T]] =
    Codec.bimap[String, ActorRef[T]](actorRefResolver.toSerializationFormat, actorRefResolver.resolveActorRef)
  given statefulDrivingEventCodec: Codec[RealTimeUserTracker.StatefulDrivingEvent] =
    deriveCodec[RealTimeUserTracker.StatefulDrivingEvent]
  given ignoreCoded: Codec[RealTimeUserTracker.Ignore.type] = deriveCodec[RealTimeUserTracker.Ignore.type]
  given aliveCheckCodec: Codec[RealTimeUserTracker.AliveCheck.type] = deriveCodec[RealTimeUserTracker.AliveCheck.type]
  given wireCodec: Codec[GroupManager.Wire] = deriveCodec[GroupManager.Wire]
  given unWireCodec: Codec[GroupManager.UnWire] = deriveCodec[GroupManager.UnWire]

  register[RealTimeUserTracker.AliveCheck.type]()
  register[RealTimeUserTracker.Ignore.type]()
  register[ClientDrivingEvent]()
  register[Session]()
  register[DrivenEvent]()
  register[RealTimeUserTracker.StatefulDrivingEvent]()
  register[GroupManager.Wire]()
  register[GroupManager.UnWire]()
