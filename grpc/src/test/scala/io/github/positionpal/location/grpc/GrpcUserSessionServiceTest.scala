package io.github.positionpal.location.grpc

import cats.data.Validated.*
import cats.effect.IO
import eu.monniot.scala3mock.scalatest.MockFactory
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.presentation.proto
import io.github.positionpal.location.application.sessions.UsersSessionService
import io.github.positionpal.location.commons.ConfigurationError
import io.github.positionpal.location.domain.{GPSLocation, RoutingMode, SampledLocation, Session, Tracking, UserState}
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import scala.language.postfixOps

class GrpcUserSessionServiceTest extends AnyWordSpec with Matchers with MockFactory {

  import Utils.*

  val fakeSessionService = mock[UsersSessionService[IO]]

//  "User sessions gRPC API service" when:
//    "attempting to get the current position of a user" when:
//      "the user have been tracked at least one time" should:
//        "return the last known position" in:
//          val result = withExpectations() {
//            when(fakeSessionService.ofUser) expects testUser returns IO(Some(testSession))
//            val service = GrpcUserSessionService[IO](fakeSessionService)
//            for
//              _ <- grpcServerFrom(service).start
//              response <- managedChannelRes.use(_.getCurrentLocation(testUser, Metadata()))
//            yield response
//          }.unsafeRunSync()
//          result.status.map(_.code) shouldBe Some(proto.StatusCode.OK)
//          result.location.map(protoToLocation(_)) shouldBe Some(testSampledLocation.position)
//
//      "otherwise" should:
//        "return a not found response" in:
//          val result = withExpectations() {
//            when(fakeSessionService.ofUser) expects testUser returns IO(None)
//            val service = GrpcUserSessionService[IO](fakeSessionService)
//            for
//              _ <- grpcServerFrom(service).start
//              response <- managedChannelRes.use(_.getCurrentLocation(testUser, Metadata()))
//            yield response
//          }.unsafeRunSync()
//          result.status.map(_.code) shouldBe Some(proto.StatusCode.NOT_FOUND)
//          result.location shouldBe None

  "User sessions gRPC API service" when:
    "attempting to get the current session of a group" should:
      "return a stream of the group's members sessions"

  private object Utils:
    val grpcLocalConfiguration = GrpcServer.Configuration[IO](50001)
    def testUser = UserId.create("luke")
    def testGroup = GroupId.create("astro")
    val testSampledLocation = SampledLocation(Instant.now(), testUser, GPSLocation(150.0, 901.0))
    val targetLocation = GPSLocation(200.0, 900.0)
    def testSession = Session.from(
      testUser,
      UserState.Routing,
      Some(testSampledLocation),
      Some(Tracking.withMonitoring(RoutingMode.Driving, targetLocation, Instant.now().plusSeconds(3600L), List(testSampledLocation)))
    )
    def grpcServerFrom(service: GrpcUserSessionService[IO]): IO[Nothing] =
      grpcLocalConfiguration.flatMap:
        case Valid(c) => proto.UserSessionServiceFs2Grpc.bindServiceResource[IO](service)
          .flatMap(s => GrpcServer.start[IO](???, Set(s)))
          .evalMap(s => IO(s.start()))
          .useForever
        case Invalid(e) => IO.raiseError(new RuntimeException(e.toString))

    val managedChannelRes = NettyChannelBuilder
      .forAddress("127.0.0.1", 50051)
      .usePlaintext()
      .resource[IO]
      .flatMap(ch => proto.UserSessionServiceFs2Grpc.stubResource[IO](ch))
  end Utils
}
