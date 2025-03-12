package io.github.positionpal.location.application.sessions

import java.time.Instant

import io.github.positionpal.location.domain.*

/** The reading model projection for [[Session]]s. It encapsulates the read-side
  * operations for querying and retrieving [[Session]]s from the underlying store.
  * @tparam F the effect type
  */
trait UserSessionsReader[F[_]]:

  /** @return the current [[Session]] of the given [[userId]] if it exists. */
  def sessionOf(scope: Scope): F[Option[Session]]

/** The writing model projection for [[Session]]s. It encapsulates the write-side
  * operations for saving [[Session]]s to the underlying store.
  * @tparam F the effect type
  * @tparam T the result type of the write operation
  */
trait UserSessionsWriter[F[_], T]:

  /** Save the given [[variation]] in the store. */
  def update(variation: Session.Snapshot): F[T]

  /** Save new route information for the given [[scope]], including the [[mode]]
    * of routing to the [[destination]] and the [[expectedArrival]].
    */
  def addRoute(scope: Scope, mode: RoutingMode, destination: Address, expectedArrival: Instant): F[T]

  /** Remove the route information for the given [[scope]]. */
  def removeRoute(scope: Scope): F[T]

/** A store for [[Session]]s, supporting both read and write operations.
  * @tparam F the effect type
  * @tparam T the result type of the write operation
  */
trait UserSessionsStore[F[_], T] extends UserSessionsReader[F] with UserSessionsWriter[F, T]
