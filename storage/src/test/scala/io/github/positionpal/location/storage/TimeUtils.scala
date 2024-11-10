package io.github.positionpal.location.storage

object TimeUtils:
  import java.time.Instant
  import java.time.temporal.ChronoUnit

  def now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
end TimeUtils
