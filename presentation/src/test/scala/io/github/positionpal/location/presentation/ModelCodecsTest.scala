package io.github.positionpal.location.presentation

import java.time.Instant

import io.bullet.borer.Cbor
import io.github.positionpal.location.domain.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ModelCodecsTest extends AnyFlatSpec with Matchers with ModelCodecs:

  "`Tracking`" should "be correctly serialized and deserialized" in:
    val user = UserId("test-user")
    val route = List(
      SampledLocation(Instant.now(), user, GPSLocation(44.139, 12.243)),
      SampledLocation(Instant.now(), user, GPSLocation(44.140, 12.244)),
    )
    val serialized = Cbor.encode(Tracking(route)).toByteArray
    val deserialized = Cbor.decode(serialized).to[Tracking].value
    deserialized.route shouldBe route

  "`MonitorableTracking`" should "be correctly serialized and deserialized" in:
    val user = UserId("test-user")
    val route = List(
      SampledLocation(Instant.now(), user, GPSLocation(44.139, 12.243)),
      SampledLocation(Instant.now(), user, GPSLocation(44.140, 12.244)),
    )
    val expectedArrival = Instant.now()
    val destination = GPSLocation(44.141, 12.245)
    val tracking = Tracking.withMonitoring(RoutingMode.Driving, destination, expectedArrival, route)
    val serialized = Cbor.encode(tracking).toByteArray
    val deserialized = Cbor.decode(serialized).to[MonitorableTracking].value
    deserialized.route shouldBe route
    deserialized.destination shouldBe destination
    deserialized.mode shouldBe RoutingMode.Driving
    deserialized.expectedArrival shouldBe expectedArrival
