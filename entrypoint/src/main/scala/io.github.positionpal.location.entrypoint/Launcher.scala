package io.github.positionpal.location.entrypoint

import io.github.positionpal.location.tracking.projections.UserSessionProjection
import cats.mtl.Handle.handleForApplicativeError
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import io.github.positionpal.location.messages.notifications.RabbitMQNotificationsPublisher
import io.github.positionpal.location.tracking.utils.{AkkaUtils, HTTPUtils}
import cats.data.{Validated, ValidatedNec}
import io.github.positionpal.location.presentation.proto.UserSessionServiceFs2Grpc
import io.github.positionpal.location.messages.groups.RabbitMQGroupsEventConsumer.RabbitMQGroupsEventConsumerImpl
import akka.management.scaladsl.AkkaManagement
import io.github.positionpal.location.messages.{MessageBrokerConnectionFactory, RabbitMQ}
import io.github.positionpal.location.grpc.{GrpcServer, GrpcUserSessionService}
import akka.actor.typed.scaladsl.Behaviors
import io.github.positionpal.location.storage.CassandraConnectionFactory
import io.github.positionpal.location.messages.groups.RabbitMQGroupsEventConsumer
import io.github.positionpal.location.tracking.{ActorBasedRealTimeTracking, MapboxService}
import io.github.positionpal.location.storage.sessions.CassandraUserSessionStore
import com.typesafe.config.ConfigFactory
import io.github.positionpal.location.application.groups.impl.BasicUserGroupsService
import io.github.positionpal.location.ws.HttpService
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.messages.notifications.RabbitMQNotificationsPublisher.RabbitMQNotificationsPublisherImpl
import io.github.positionpal.location.storage.groups.CassandraUserGroupsStore
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.application.sessions.impl.BasicUsersSessionService
import io.github.positionpal.location.commons.{ConfigurationError, EnvVariablesProvider}
import akka.management.cluster.bootstrap.ClusterBootstrap
import cats.effect.{IO, IOApp, Resource}

object Launcher extends IOApp.Simple:

  private val logger = LoggerFactory.getLogger(getClass)

  override def run: IO[Unit] = app.use(_ => IO.never)

  private val app: Resource[IO, Unit] = for
    mapsService <- configureMapsService()
    notificationService <- Resource.eval(RabbitMQNotificationsPublisher[IO]())
    actorSystem <- configureTrackingService(mapsService, notificationService)
    given ActorSystem[?] = actorSystem
    _ <- Resource.eval(IO.fromFuture(IO(AkkaManagement(actorSystem).start())))
    _ <- Resource.eval(IO(ClusterBootstrap(actorSystem).start()))
    cassandraConnection <- Resource.pure(CassandraConnectionFactory[IO](actorSystem).get)
    userGroupsStore <- Resource.eval(CassandraUserGroupsStore[IO](cassandraConnection))
    userSessionsStore <- Resource.eval(CassandraUserSessionStore[IO](cassandraConnection))
    userGroupsService = BasicUserGroupsService[IO](userGroupsStore)
    groupsEventService <- Resource.eval(RabbitMQGroupsEventConsumer[IO](userGroupsService))
    sessionService <- Resource
      .eval(IO(GrpcUserSessionService[IO](BasicUsersSessionService[IO](userGroupsService, userSessionsStore))))
    _ <- Resource.eval(IO(UserSessionProjection.init(actorSystem, userSessionsStore)))
    _ <- configureGrpcServices(sessionService)
    _ <- configureQueueServices(groupsEventService, notificationService)
    _ <- Resource.eval(IO(logger.info("All services have been started.")))
  yield ()

  private def configureMapsService(): Resource[IO, MapsService[IO]] =
    for
      httpClient <- HTTPUtils.clientRes
      envs <- Resource.eval(EnvVariablesProvider[IO].configuration)
      mapsConfig <- Resource.eval(IO.pure(MapboxService.Configuration(httpClient, envs("MAPBOX_API_KEY"))))
      maps <- Resource.eval(MapboxService[IO](mapsConfig))
    yield maps

  private def configureTrackingService(maps: MapsService[IO], notifier: NotificationService[IO]) =
    for
      envs <- Resource.eval(EnvVariablesProvider[IO].configuration)
      config = if envs.get("PRODUCTION").fold(true)(_ == "true") then "akka.conf" else "akka-local.conf"
      _ <- Resource.eval(IO(logger.info(s"Akka configuration file: $config")))
      actorSystem <- AkkaUtils.startup[IO, Any](ConfigFactory.load(config))(Behaviors.empty)
      given ActorSystem[?] = actorSystem
      trackingService <- Resource.eval(ActorBasedRealTimeTracking.Service[IO](actorSystem, notifier, maps))
      validatedHttpConfig <- Resource.eval(HttpService.Configuration.fromEnv[IO])
      httpConfig <- Resource.eval(validatedHttpConfig.get)
      _ <- HttpService.start[IO](httpConfig)(trackingService)
    yield actorSystem

  private def configureQueueServices(
      groupsEventConsumer: RabbitMQGroupsEventConsumerImpl[IO],
      notificationService: RabbitMQNotificationsPublisherImpl[IO],
  ) = for
    validatedRabbitMQConfig <- Resource.eval(RabbitMQ.Configuration.fromEnv[IO])
    rabbitMQConfig <- Resource.eval(validatedRabbitMQConfig.get)
    rabbitMQConnection <- MessageBrokerConnectionFactory.ofRabbitMQ[IO](rabbitMQConfig)
    _ <- Resource.eval(groupsEventConsumer.start(rabbitMQConnection).start)
    _ <- Resource.eval(notificationService.start(rabbitMQConnection).start)
  yield ()

  private def configureGrpcServices(sessionService: GrpcUserSessionService[IO]) =
    for
      validatedGrpcConfiguration <- Resource.eval(GrpcServer.Configuration.fromEnv[IO])
      grpcConfig <- Resource.eval(validatedGrpcConfiguration.get)
      _ <- Resource.eval:
        UserSessionServiceFs2Grpc
          .bindServiceResource[IO](sessionService)
          .flatMap(s => GrpcServer.start[IO](grpcConfig, Set(s)))
          .evalMap(s => IO(s.start()))
          .useForever
          .start
    yield ()

  extension [A](v: ValidatedNec[ConfigurationError, A])
    private def get: IO[A] = v match
      case Validated.Valid(a) => IO.pure(a)
      case Validated.Invalid(e) => IO.raiseError(new Exception(e.map(_.message).toNonEmptyList.toList.mkString(", ")))
