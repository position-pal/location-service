package io.github.positionpal.location.application.storage

import io.github.positionpal.location.domain.{DrivingEvent, UserId, Session}

trait UserSessionReader[F[_]]:
  def sessionOf(userId: UserId): F[Session]

trait UserSessionWriter[F[_], T]:
  def update(eventVariation: DrivingEvent): F[T]

trait UserSessionStore[F[_], T] extends UserSessionReader[F] with UserSessionWriter[F, T]
