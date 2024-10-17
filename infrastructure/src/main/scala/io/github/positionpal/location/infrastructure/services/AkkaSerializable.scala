package io.github.positionpal.location.infrastructure.services

import akka.actor.typed.ActorSystem

/** Marker trait for remote serializable messages exchanged between distributed actors.
  * Every message class exchanged between distributed actors should extend this trait
  * (which is referenced as binding in the Akka configuration).
  */
trait AkkaSerializable

trait AkkaActorSystemProvider[-T]:
  def system: ActorSystem[T]
