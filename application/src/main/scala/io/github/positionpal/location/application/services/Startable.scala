package io.github.positionpal.location.application.services

/** A type class representing an active component that can be started.
  * @tparam F the effect type
  * @tparam U the result type of the start operation
  */
trait Startable[F[_], U]:
  def start: F[U]
