package io.github.positionpal.location.tracking.actors

/** Marker trait for remote serializable messages exchanged between distributed actors.
  * Every message class exchanged between distributed actors or persisted should extend this trait.
  * This trait should be referenced as binding in the Akka serialization configuration.
  */
trait AkkaSerializable
