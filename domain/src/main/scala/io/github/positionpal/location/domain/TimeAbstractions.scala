package io.github.positionpal.location.domain

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

extension (instant: Instant)
  def format(using formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")): String =
    formatter.withZone(ZoneId.of("UTC")).format(instant)
