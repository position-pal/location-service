package io.github.positionpal.location.presentation

import java.time.Instant

import io.bullet.borer.Cbor
import io.github.positionpal.location.domain.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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
  val user: UserId = UserId("test-user")
  val route: Route = SampledLocation(Instant.now(), user, GPSLocation(44.139, 12.243)) ::
    SampledLocation(Instant.now(), user, GPSLocation(44.140, 12.244)) :: Nil
  val tracking: Tracking = Tracking(route)
  val monitorableTracking: MonitorableTracking =
    Tracking.withMonitoring(RoutingMode.Driving, GPSLocation(44.141, 12.245), Instant.now(), route)
  val session: Session = Session.from(user, UserState.Active, None, Some(monitorableTracking))
