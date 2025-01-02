package io.github.positionpal.location.messages

object RabbitMQ:

  /** Configuration for connecting to a RabbitMQ broker. */
  trait Configuration:
    /** The host RabbitMQ broker is running on. */
    def host: String

    /** The port RabbitMQ broker is listening on. */
    def port: Int

    /** The username to authenticate with the RabbitMQ broker. */
    def username: String

    /** The password to authenticate with the RabbitMQ broker. */
    def password: String

    /** The virtual host to connect to on the RabbitMQ broker. */
    def virtualHost: String

  object Configuration:

    import cats.data.ValidatedNec
    import cats.effect.kernel.Sync
    import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxTuple5Semigroupal, catsSyntaxValidatedIdBinCompat0}
    import io.github.positionpal.location.commons.ConfigurationError
    import io.github.positionpal.location.commons.ConfigurationError.*
    import io.github.positionpal.location.commons.ScopeFunctions.let

    /** Create a new [[Configuration]] instance with the given parameters.
      * @param host the host RabbitMQ broker is running on
      * @param port the port RabbitMQ broker is listening on
      * @param username the username to authenticate with the RabbitMQ broker
      * @param password the password to authenticate with the RabbitMQ broker
      * @param virtualHost the virtual host to connect to on the RabbitMQ broker
      * @tparam F the effect type
      * @return a [[ValidatedNec]] instance containing either a valid [[Configuration]] or a
      */
    def apply[F[_]: Sync](
        host: String,
        port: Int,
        username: String,
        password: String,
        virtualHost: String,
    ): F[ValidatedNec[ConfigurationError, Configuration]] =
      Sync[F]
        .delay:
          (host.nonEmpty, port.positive, username.nonEmpty, password.nonEmpty, virtualHost.nonEmpty).mapN(Impl.apply)
        .handleError(e => Invalid(e.toString).invalidNec)

    /** Create a new [[Configuration]] instance with the parameters read from environment variables,
      * expected in `RABBITMQ_<PARAMETER>` format.
      * @return a [[ValidatedNec]] instance containing either a valid [[Configuration]] or a
      *         [[ConfigurationError]] in case of missing or invalid environment variables.
      */
    def fromEnv[F[_]: Sync]: F[ValidatedNec[ConfigurationError, Configuration]] =
      Sync[F]
        .delay:
          (
            "RABBITMQ_HOST".let(s => sys.env.get(s).validStr(s)),
            "RABBITMQ_PORT".let(s => sys.env.get(s).validStr(s).andThen(_.toInt.positive)),
            "RABBITMQ_USERNAME".let(s => sys.env.get(s).validStr(s)),
            "RABBITMQ_PASSWORD".let(s => sys.env.get(s).validStr(s)),
            "RABBITMQ_VIRTUAL_HOST".let(s => sys.env.get(s).validStr(s)),
          ).mapN(Impl.apply)
        .handleError(e => Invalid(e.toString).invalidNec)

    private case class Impl(host: String, port: Int, username: String, password: String, virtualHost: String)
        extends Configuration

  import cats.effect.kernel.{Resource, Temporal}
  import cats.effect.std.Console
  import com.comcast.ip4s.{Host, Port}
  import fs2.io.net.Network
  import lepus.client.{Connection, LepusClient}
  import lepus.protocol.domains.Path
  import io.github.positionpal.location.commons.ConnectionFactory

  private[messages] class ConnectionFactoryImpl[F[_]: Temporal: Network: Console](
      configuration: Configuration,
  ) extends ConnectionFactory[[C] =>> Resource[F, C], Connection[F]]:
    def get: Resource[F, Connection[F]] = LepusClient[F](
      host = Host.fromString(configuration.host).getOrElse(throw new RuntimeException("Invalid host")),
      port = Port.fromInt(configuration.port).getOrElse(throw new RuntimeException("Invalid port")),
      username = configuration.username,
      password = configuration.password,
      vhost = Path.from(configuration.virtualHost).getOrElse(Path("/")),
    )

  /** An ADT representing the possible errors that can occur when interacting with RabbitMQ client. */
  enum Error(msg: String) extends RuntimeException(msg):
    case QueueDeclarationFailed extends Error("Queue declaration failed")

  trait Protocol:
    import lepus.protocol.domains.{ExchangeName, QueueName, ShortString}

    /** The name of the exchange where group events are published by the user-group microservice. */
    val groupsEventsExchange: ExchangeName = ExchangeName("group_updates_exchange")

    /** The name of the exchange where notifications commands are published by the location service. */
    val notificationsCommandExchange: ExchangeName = ExchangeName("push-notifications")

    /** The name of the queue where group events are collected. */
    val groupsEventsQueue: QueueName = QueueName("group_updates_location_service")

    /** The key used in the header to identify the type of message. */
    val msgTypeKey: ShortString = ShortString("message_type")

  trait Utils extends Protocol:
    import lepus.client.Message
    import lepus.protocol.domains.{FieldData, FieldTable, ShortString}
    import lepus.protocol.classes.basic.Properties
    export Error.*

    extension (s: String) def asShortOrEmpty: ShortString = ShortString.from(s).getOrElse(ShortString.empty)

    type Headers = FieldTable

    extension (expected: ShortString)
      infix def in(headers: Headers, key: Option[ShortString] = Some(msgTypeKey)): Boolean =
        key match
          case Some(k) => headers.get(k).contains(expected)
          case None => headers.values.values.exists(_ == expected)

    extension (b: Array[Byte])
      def toMessage(headersData: Map[ShortString, FieldData]): Message[Array[Byte]] =
        Message(b, Properties(headers = Some(FieldTable(headersData))))
