package io.github.positionpal.location.presentation

import java.time.Instant

import org.scalatest.flatspec.AnyFlatSpec
import io.bullet.borer.Cbor
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.location.domain.GeoUtils.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.{GroupId, UserId}

class ModelCodecsTest extends AnyFlatSpec with Matchers with ModelCodecs:

  import ModelCodecsTest.*

  "`Tracking`" should "be correctly serialized and deserialized" in:
    val serialized = Cbor.encode(tracking).toByteArray
    val deserialized = Cbor.decode(serialized).to[Tracking].value
    deserialized shouldBe tracking

  "`MonitorableTracking`" should "be correctly serialized and deserialized" in:
    val serialized = Cbor.encode(monitorableTracking).toByteArray
    val deserialized = Cbor.decode(serialized).to[MonitorableTracking].value
    deserialized shouldBe monitorableTracking

  "`Session`" should "be correctly serialized and deserialized" in:
    val serialized = Cbor.encode(session).toByteArray
    val deserialized = Cbor.decode(serialized).to[Session].value
    deserialized shouldBe session

private object ModelCodecsTest:
  val user: UserId = UserId.create("luke")
  val group: GroupId = GroupId.create("astro")
  val route: Route = SampledLocation(Instant.now(), user, group, bolognaCampus.position) ::
    SampledLocation(Instant.now(), user, group, forliCampus.position) :: Nil
  val tracking: Tracking = Tracking(route)
  val monitorableTracking: MonitorableTracking =
    Tracking.withMonitoring(RoutingMode.Driving, cesenaCampus, Instant.now(), route)
  val session: Session = Session.from(group, user, UserState.Active, None, Some(monitorableTracking))
