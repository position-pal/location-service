package io.github.positionpal.location.commons

import cats.effect.IO
import cats.effect.unsafe.implicits.global

@main def myTest(): Unit =
  IO:
    throw new RuntimeException("This is a test")
  .unsafeRunAsync(_ => ())
  println("Hello, world!")
  Thread.sleep(1000)
