package io.github.positionpal.location.domain

sealed trait Alert

object Alert:
  case object Stuck extends Alert
  case object Late extends Alert
