package io.github.positionpal.location.tracking.utils

import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.effect.{IO, Resource}

object HTTPUtils:

  val clientRes: Resource[IO, Client[IO]] =
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    EmberClientBuilder.default[IO].build
