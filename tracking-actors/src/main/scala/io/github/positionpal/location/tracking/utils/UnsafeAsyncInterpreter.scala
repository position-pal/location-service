package io.github.positionpal.location.tracking.utils

import scala.concurrent.Future

import cats.effect.kernel.Async

trait UnsafeAsyncInterpreter[F[_]: Async]:
  def unsafeRun[A](fa: F[A]): Future[A]

object UnsafeAsyncInterpreter:
  import cats.effect.IO

  given UnsafeAsyncInterpreter[IO] with
    def unsafeRun[A](fa: IO[A]): Future[A] =
      import cats.effect.unsafe.implicits.global
      fa.unsafeToFuture()
