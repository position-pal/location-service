package io.github.positionpal.location.domain

object TimeUtils:
  import java.time.Instant
  import java.time.temporal.ChronoUnit

  def now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  def inTheFuture: Instant = Instant.parse("2100-12-31T23:59:59Z")
  def inThePast: Instant = Instant.parse("2000-01-01T00:00:00Z")
end TimeUtils
