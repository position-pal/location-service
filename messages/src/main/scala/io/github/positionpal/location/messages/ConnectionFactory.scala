package io.github.positionpal.location.messages

/** A factory for creating connections to a message broker.
  * @tparam F the effect type
  * @tparam C the connection type
  */
trait ConnectionFactory[F[_], C]:

  /** @return a new connection to the message broker, properly encapsulated in the effect type `F`. */
  def get: F[C]

/** A factory for creating connections to RabbitMQ message broker. */
object MessageBrokerConnectionFactory:
  import cats.effect.kernel.{Temporal, Resource}
  import cats.effect.std.Console
  import lepus.client.Connection
  import fs2.io.net.Network

  import io.github.positionpal.location.messages.RabbitMQ.Configuration

  /** Create a new connection to a RabbitMQ broker using the given `configuration`.
    * @return a [[Resource]] encapsulating the connection to the RabbitMQ broker.
    */
  def ofRabbitMQ[F[_]: Temporal: Network: Console](configuration: Configuration): Resource[F, Connection[F]] =
    RabbitMQ.ConnectionFactoryImpl[F](configuration).get
