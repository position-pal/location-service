package io.github.positionpal.location.application.services

trait StartableOps[F[_], U]:
  def start: F[U]
