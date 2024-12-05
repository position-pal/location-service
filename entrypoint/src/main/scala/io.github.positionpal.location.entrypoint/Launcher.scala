package io.github.positionpal.location.entrypoint

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import cats.data.Validated
import cats.effect.{IO, IOApp, Resource}
import cats.mtl.Handle.handleForApplicativeError
import com.typesafe.config.ConfigFactory
import io.github.positionpal.location.application.groups.impl.BasicUserGroupsService
import io.github.positionpal.location.application.sessions.impl.BasicUsersSessionService
import io.github.positionpal.location.commons.ConfigurationError
import io.github.positionpal.location.grpc.{GrpcServer, GrpcUserSessionService}
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.services.projections.UserSessionProjection
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import io.github.positionpal.location.messages.groups.RabbitMQGroupsEventConsumer
import io.github.positionpal.location.messages.{MessageBrokerConnectionFactory, RabbitMQ}
import io.github.positionpal.location.storage.CassandraConnectionFactory
import io.github.positionpal.location.storage.groups.CassandraUserGroupsStore
import io.github.positionpal.location.ws.HttpService
import io.github.positionpal.location.presentation.proto.UserSessionServiceFs2Grpc
import io.github.positionpal.location.storage.sessions.CassandraUserSessionStore

object Launcher extends IOApp.Simple:

  private val app: Resource[IO, Unit] = for
    actorSystem <- AkkaUtils.startup[IO, Any](ConfigFactory.load("akka.conf"))(Behaviors.empty)
    given ActorSystem[?] = actorSystem
    realTimeTrackingService <- Resource.eval(ActorBasedRealTimeTracking.Service[IO](actorSystem))
    _ <- HttpService.start[IO](8080)(realTimeTrackingService)
    validatedRabbitMQConfig <- Resource.eval(RabbitMQ.Configuration.fromEnv[IO])
    rabbitMQConfig <- Resource.eval:
      validatedRabbitMQConfig match
        case Validated.Valid(config) => IO.pure(config)
        case Validated.Invalid(errors) => IO.raiseError(new Exception(errors.map(_.message).toNonEmptyList.toList.mkString(", ")))
    rabbitMQConnection <- MessageBrokerConnectionFactory.ofRabbitMQ[IO](rabbitMQConfig)
    cassandraConnection <- Resource.pure(CassandraConnectionFactory[IO](actorSystem).get)
    userGroupsStore <- Resource.eval(CassandraUserGroupsStore[IO](cassandraConnection))
    userGroupsService = BasicUserGroupsService[IO](userGroupsStore)
    userSessionsStore <- Resource.eval(CassandraUserSessionStore[IO](cassandraConnection))
    _ <- Resource.eval(IO(UserSessionProjection.init(actorSystem, userSessionsStore)))
    validatedGrpcConfiguration <- Resource.eval(GrpcServer.Configuration[IO](50052))
    sessionService <- Resource.eval(IO(GrpcUserSessionService[IO](BasicUsersSessionService[IO](userGroupsService, userSessionsStore))))
    _ <- Resource.eval:
      (validatedGrpcConfiguration match
        case Validated.Valid(config) =>
          UserSessionServiceFs2Grpc.bindServiceResource[IO](sessionService)
            .flatMap(s => GrpcServer.start[IO](config, Set(s)))
            .evalMap(s => IO(s.start())).useForever
        case Validated.Invalid(errors) =>
          IO.raiseError(new Exception(errors.map(_.message).toNonEmptyList.toList.mkString(", ")))
        ).start
    _ <- Resource.eval(RabbitMQGroupsEventConsumer[IO](userGroupsService).start(rabbitMQConnection))
  yield ()

  override def run: IO[Unit] = app.use(_ => IO.never)
