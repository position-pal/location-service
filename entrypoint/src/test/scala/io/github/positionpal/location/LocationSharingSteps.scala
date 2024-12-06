package io.github.positionpal.location

import cats.effect.IO
import fs2.grpc.syntax.all.*
import io.cucumber.scala.{EN, ScalaDsl}
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.UserState.Active
import io.github.positionpal.location.domain.{SampledLocation, UserUpdate}
import io.github.positionpal.location.presentation.ProtoConversions.protoToGid
import io.github.positionpal.location.presentation.proto.UserSessionServiceFs2Grpc
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}

object LocationSharingSteps extends ScalaDsl with EN with AcceptanceTests with Matchers with ScalaFutures:

  import cats.effect.unsafe.implicits.global

  private val scenario = wsConfigurator.Scenario(
    group = astroGroup,
    clients = wsConfigurator.Client(impersonatingUserId) :: wsConfigurator.Client(robyId) :: Nil,
    events = sample(impersonatingUserId, cesenaCampus) :: sample(robyId, bolognaCampus) :: Nil,
  )

  When("I start sharing my location with a group"):
    wsConfigurator.runTest(scenario)

  Then("all connected group members are able to receive location updates"):
    val expectedEvents = scenario.events.map(_.toUserUpdate)
    eventually(timeout(Span(30, Seconds)), interval(Span(500, Milliseconds))):
      scenario.clients.foreach:
        _.responses should contain allElementsOf expectedEvents

  When("I stop sharing my location with the group") {}

  Then("all connected group members are no longer able to receive location updates") {}

  But("they can still get my last known location and state"):
    Thread.sleep(20_000)
    val sessionsService = NettyChannelBuilder.forAddress("127.0.0.1", 50052).usePlaintext().resource[IO]
      .flatMap(ch => UserSessionServiceFs2Grpc.stubResource[IO](ch))
    val result = sessionsService.use: s =>
      s.getCurrentSession(astroGroup, Metadata()).compile.toList
    .unsafeRunSync()
    println(s">>>> $result")

  extension (e: SampledLocation)
    def toUserUpdate: UserUpdate = UserUpdate(e.timestamp, e.user, Some(e.position), Active)
