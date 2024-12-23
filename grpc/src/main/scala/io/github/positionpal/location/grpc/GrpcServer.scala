package io.github.positionpal.location.grpc

import scala.jdk.CollectionConverters.given

import cats.effect.kernel.{Async, Resource}
import fs2.grpc.syntax.all.fs2GrpcSyntaxServerBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.{Server, ServerServiceDefinition}

/** The gRPC server entry point. */
object GrpcServer:

  /** Configuration for the gRPC server. */
  trait Configuration:
    /** The port the server should listen on. */
    def port: Int

  object Configuration:

    import cats.effect.kernel.Sync
    import cats.data.ValidatedNec
    import io.github.positionpal.location.commons.ConfigurationError
    import io.github.positionpal.location.commons.ConfigurationError.*
    import io.github.positionpal.location.commons.ScopeFunctions.*
    import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxValidatedIdBinCompat0}

    /** Create a new [[Configuration]] instance.
      * @param port the port the server should listen on.
      * @return a [[Validated]] instance containing either a valid [[Configuration]] or a [[ConfigurationError]].
      */
    def apply[F[_]: Sync](port: Int): F[ValidatedNec[ConfigurationError, Configuration]] = Sync[F].delay:
      port.positive.map(BasicGrpcConfiguration.apply)
    .handleError(e => Invalid(e.toString).invalidNec)

    /** Create a new [[Configuration]] instance with the parameters read from environment variables,
      * expected in `GRPC_<PARAMETER>` format.
      * @return a [[Validated]] instance containing either a valid [[Configuration]] or a [[ConfigurationError]].
      */
    def fromEnv[F[_]: Sync]: F[ValidatedNec[ConfigurationError, Configuration]] = Sync[F].delay:
      "GRPC_PORT".let(s => sys.env.get(s).validStr(s).andThen(_.toInt.positive)).map(BasicGrpcConfiguration.apply)
    .handleError(e => Invalid(e.toString).invalidNec)

    private case class BasicGrpcConfiguration(port: Int) extends Configuration

  /** Startup a new gRPC server configured as per the given [[Configuration]] and wired with the given [[services]].
    * @return a [[Resource]] encapsulating a [[Server]] instance in the `F` effect type context,
    *         that will start and stop the server when acquired and released, respectively.
    */
  def start[F[_]: Async](configuration: Configuration, services: Set[ServerServiceDefinition]): Resource[F, Server] =
    NettyServerBuilder.forPort(configuration.port).addServices(services.toList.asJava).resource[F]
