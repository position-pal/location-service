package io.github.positionpal.location.grpc

import scala.language.postfixOps

import cats.effect.IO
import cats.effect.kernel.Resource
import eu.monniot.scala3mock.cats.withExpectations
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.application.sessions.UsersSessionService
import io.github.positionpal.location.domain.Session
import io.github.positionpal.location.presentation.ProtoConversions.given
import io.github.positionpal.location.presentation.proto
import io.github.positionpal.location.presentation.proto.UserSessionServiceFs2Grpc
import io.grpc.Metadata
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GrpcUserSessionServiceTest extends AnyWordSpec with Matchers with MockFactory:

  import Utils.*
  import cats.effect.unsafe.implicits.global

  val fakeSessionService: UsersSessionService[IO] = mock[UsersSessionService[IO]]

  "User sessions gRPC API service" when:
    "attempting to get the current session of a group" should:
      "return a stream of the group's members sessions" in:
        val result = withExpectations() {
          when(fakeSessionService.ofGroup) expects sessions._1 returns fs2.Stream.emits[IO, Session](sessions._2)
          for
            _ <- grpcServerFrom(GrpcUserSessionService[IO](fakeSessionService)).start
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
    import io.github.positionpal.location.domain.{UserState, SampledLocation, GPSLocation}
    import io.github.positionpal.location.domain.UserState.*
    import io.github.positionpal.location.presentation.proto
    import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
    import java.time.Instant.now

    val sessions: (GroupId, List[Session]) = (
      GroupId.create("astro"),
      UserId.create("eve").let(u => Session.from(u, Active, Some(SampledLocation(now(), u, GPSLocation(15, 90))), None))
        :: UserId.create("luke").let(u => Session.from(u, Inactive, None, None))
        :: Nil,
    )

    val port = 50052
    val grpcLocalConfiguration = GrpcServer.Configuration[IO](port)
    def grpcServerFrom(service: GrpcUserSessionService[IO]): IO[Nothing] =
      grpcLocalConfiguration.flatMap:
        case Valid(c) =>
          UserSessionServiceFs2Grpc.bindServiceResource[IO](service).flatMap(s => GrpcServer.start[IO](c, Set(s)))
            .evalMap(s => IO(s.start())).useForever
        case Invalid(e) => IO.raiseError(new RuntimeException(e.toString))
    val managedChannelRes: Resource[IO, UserSessionServiceFs2Grpc[IO, Metadata]] =
      NettyChannelBuilder.forAddress("127.0.0.1", port).usePlaintext().resource[IO]
        .flatMap(ch => UserSessionServiceFs2Grpc.stubResource[IO](ch))
  end Utils
