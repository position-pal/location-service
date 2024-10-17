package io.github.positionpal.location.application.storage

import io.github.positionpal.location.domain.{SampledLocation, UserId}

/** The reading model projection for [[SampledLocation]]s.
  * It encapsulates the read-side operations for querying and retrieving
  * [[SampledLocation]]s from the underlying store.
  */
trait TrackingEventsReader[F[_]]:

  /** @return the last [[SampledLocation]] of the given [[user]]. */
  def lastOf(user: UserId): F[Option[SampledLocation]]

/** The writing model projection for [[SampledLocation]]s.
  * It encapsulates the write-side operations for saving [[SampledLocation]]s
  * to the underlying store.
  */
trait TrackingEventsWriter[F[_], T]:

  /** Save the given [[trackingEvent]] in the store. */
  def save(trackingEvent: SampledLocation): F[T]

/** A store for [[SampledLocation]]s, supporting both read and write operations. */
trait TrackingEventsStore[F[_], T] extends TrackingEventsReader[F] with TrackingEventsWriter[F, T]
