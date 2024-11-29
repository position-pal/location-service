package io.github.positionpal.location.messages

import cats.data.Validated.*
import cats.effect.{IO, Resource}
import lepus.client.Connection

object RabbitMQTestUtils:

  val configuration = RabbitMQ.Configuration[IO](
    host = "localhost",
    port = 5672,
    username = "guest",
    password = "admin",
    virtualHost = "/",
  )

  val connection: Resource[IO, Connection[IO]] = Resource.eval(configuration).flatMap:
    case Valid(config) => MessageBrokerConnectionFactory.ofRabbitMQ[IO](config)
    case Invalid(errors) => Resource.eval(IO.raiseError(new Exception(errors.toNonEmptyList.toList.mkString(", "))))
