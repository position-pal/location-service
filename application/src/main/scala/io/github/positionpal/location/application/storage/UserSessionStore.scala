package io.github.positionpal.location.application.storage

import io.github.positionpal.location.domain.{UserId, Session}

trait UserSessionReader[F[_]]:
  def sessionOf(userId: UserId): F[Option[Session]]

trait UserSessionWriter[F[_], T]:
  def update(variation: Session.Snapshot): F[T]

trait UserSessionStore[F[_], T] extends UserSessionReader[F] with UserSessionWriter[F, T]
