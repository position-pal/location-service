package io.github.positionpal.location.commons

/** A generic factory of connections.
  * @tparam F the effect type in which the connection is encapsulated
  * @tparam C the connection type
  */
trait ConnectionFactory[F[_], C]:

  /** @return a new connection [[C]], properly encapsulated in the effect type `F`. */
  def get: F[C]
