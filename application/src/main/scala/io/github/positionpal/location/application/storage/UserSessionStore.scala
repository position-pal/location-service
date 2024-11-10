package io.github.positionpal.location.application.storage

import io.github.positionpal.location.domain.{Session, UserId}

/** The reading model projection for [[Session]]s. It encapsulates the read-side
  * operations for querying and retrieving [[Session]]s from the underlying store.
  * @tparam F the effect type
  */
trait UserSessionReader[F[_]]:

  /** @return the current [[Session]] of the given [[userId]] if it exists. */
  def sessionOf(userId: UserId): F[Option[Session]]

/** The writing model projection for [[Session]]s. It encapsulates the write-side
  * operations for saving [[Session]]s to the underlying store.
  * @tparam F the effect type
  * @tparam T the result type of the write operation
  */
trait UserSessionWriter[F[_], T]:

  /** Save the given [[variation]] in the store. */
  def update(variation: Session.Snapshot): F[T]

/** A store for [[Session]]s, supporting both read and write operations.
  * @tparam F the effect type
  * @tparam T the result type of the write operation
  */
trait UserSessionStore[F[_], T] extends UserSessionReader[F] with UserSessionWriter[F, T]
