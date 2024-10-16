package io.github.positionpal.location.application.storage

import io.github.positionpal.location.application.services.UserState
import io.github.positionpal.location.domain.UserId

/** The reading model projection for [[UserState]].
  * It encapsulates the read-side operations for querying and retrieving
  * [[UserState]]s from the underlying store.
  */
trait UserStateReader[F[_]]:
  def currentState(userId: UserId): F[UserState]

/** The writing model projection for [[UserState]]s.
  * It encapsulates the write-side operations for saving [[UserState]]s
  * to the underlying store.
  */
trait UserStateWriter[F[_], T]:
  def update(currentState: UserState): F[T]

/** A store for [[UserState]]s, supporting both read and write operations. */
trait UserStateStore[F[_], T] extends UserStateReader[F] with UserStateWriter[F, T]
