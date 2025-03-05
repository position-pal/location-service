package io.github.positionpal.location.grpc

import scala.language.postfixOps

import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.presentation.ProtoConversions.given
import eu.monniot.scala3mock.cats.withExpectations
import io.github.positionpal.location.presentation.proto.UserSessionsServiceFs2Grpc
import io.grpc.Metadata
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.location.domain.Session
import cats.effect.kernel.Resource
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.IO
import io.github.positionpal.location.presentation.proto
import io.github.positionpal.location.application.sessions.UserSessionsService

class GrpcUserSessionsServiceTest extends AnyWordSpec with Matchers with MockFactory:

  import Utils.*
  import cats.effect.unsafe.implicits.global

  val fakeSessionService: UserSessionsService[IO] = mock[UserSessionsService[IO]]

  "User sessions gRPC API service" when:
    "attempting to get the current session of a group" should:
      "return a stream of the group's members sessions" in:
        val result = withExpectations() {
          when(fakeSessionService.ofGroup) expects sessions._1 returns fs2.Stream.emits[IO, Session](sessions._2)
          for
            _ <- grpcServerFrom(GrpcUserSessionsService[IO](fakeSessionService)).start
            response <- managedChannelRes.use(_.getCurrentSession(sessions._1, Metadata()).compile.toList)
          yield response
        }.unsafeRunSync()
        result.foreach(_.status.map(_.code) shouldBe Some(proto.StatusCode.OK))
        result.map(_.session) shouldBe sessions._2.map(Some(_)).map(_.map(sessionToProto(_)))

  private object Utils:
    import cats.data.Validated.*
    import fs2.grpc.syntax.all.*
    import io.github.positionpal.location.commons.ScopeFunctions.*
    import io.github.positionpal.entities.{GroupId, UserId}
    import io.github.positionpal.location.domain.{SampledLocation, Scope, UserState}
    import io.github.positionpal.location.domain.UserState.*
    import io.github.positionpal.location.presentation.proto
    import io.github.positionpal.location.domain.GeoUtils.*
    import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
    import java.time.Instant.now

    val testGroup = GroupId.create("astro")
    val eveScope = Scope(UserId.create("eve"), testGroup)
    val lukeScope = Scope(UserId.create("luke"), testGroup)
    val sessions: (GroupId, List[Session]) = (
      testGroup,
      eveScope.let(s => Session.from(s, Active, Some(SampledLocation(now(), s, bolognaCampus.location)), None))
        :: lukeScope.let(s => Session.from(s, Inactive, None, None))
        :: Nil,
    )

    val port = 50052
    val grpcLocalConfiguration = GrpcServer.Configuration[IO](port)
    def grpcServerFrom(service: GrpcUserSessionsService[IO]): IO[Nothing] =
      grpcLocalConfiguration.flatMap:
        case Valid(c) =>
          UserSessionsServiceFs2Grpc
            .bindServiceResource[IO](service)
            .flatMap(s => GrpcServer.start[IO](c, Set(s)))
            .evalMap(s => IO(s.start()))
            .useForever
        case Invalid(e) => IO.raiseError(new RuntimeException(e.toString))
    val managedChannelRes: Resource[IO, UserSessionsServiceFs2Grpc[IO, Metadata]] =
      NettyChannelBuilder
        .forAddress("127.0.0.1", port)
        .usePlaintext()
        .resource[IO]
        .flatMap(ch => UserSessionsServiceFs2Grpc.stubResource[IO](ch))
  end Utils
