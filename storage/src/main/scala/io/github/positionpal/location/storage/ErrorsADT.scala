package io.github.positionpal.location.storage

/** A trait that represents a store error. */
trait StoreError(val reason: String) extends RuntimeException:
  override def getMessage: String = reason

/** A fallback generic error for database failures. */
final case class DatabaseError(message: String) extends StoreError(message)
