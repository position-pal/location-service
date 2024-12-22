package io.github.positionpal.location.storage.sessions

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import io.github.positionpal.location.storage.CassandraConnectionFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserSessionStoreTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike with Matchers:

  import cats.effect.unsafe.implicits.global
  import cats.effect.{IO, Resource}
  import cats.mtl.Handle.handleForApplicativeError
  import io.github.positionpal.entities.{GroupId, UserId}
  import io.github.positionpal.location.domain.*
  import io.github.positionpal.location.domain.Session.Snapshot
  import io.github.positionpal.location.domain.UserState.*
  import io.github.positionpal.location.domain.TimeUtils.*
  import io.github.positionpal.location.domain.GeoUtils.*
  import org.scalatest.OptionValues.convertOptionToValuable

  private val connection = CassandraConnectionFactory[IO](system).get
  private val storeResource = Resource.eval(CassandraUserSessionStore[IO](connection))

  "User Session Database" when:
    "attempting to get the latest updated session of an unknown user" should:
      "return None" in:
        storeResource.use:
          _.sessionOf(Scope(UserId.create("unknown-user"), GroupId.create("unknown-group")))
        .unsafeRunSync() shouldBe None

    val scope = Scope(UserId.create("luke"), GroupId.create("astro"))
    val initialVariation = Snapshot(scope, Active, Some(SampledLocation(now, scope, bolognaCampus)))
    val lastVariation = Snapshot(scope, Active, Some(SampledLocation(now, scope, imolaCampus)))
    val routingVariations = Snapshot(scope, Routing, Some(SampledLocation(now, scope, ravennaCampus))) ::
      Snapshot(scope, Routing, Some(SampledLocation(now.plusSeconds(1), scope, forliCampus))) ::
      Snapshot(scope, SOS, Some(SampledLocation(now.plusSeconds(2), scope, cesenaCampus))) ::
      Snapshot(scope, SOS, Some(SampledLocation(now.plusSeconds(3), scope, riminiCampus))) :: Nil

    "receiving a variation of an active user" should:
      "record their state and last known location" in:
        updateAndGet(scope, initialVariation).unsafeRunSync().map(_.toSnapshot) shouldBe Some(initialVariation)

      "update their state if already present" in:
        updateAndGet(scope, lastVariation).unsafeRunSync().map(_.toSnapshot) shouldBe Some(lastVariation)

    "receiving a variation of an inactive user" should:
      "record their new state despite leaving unaltered the last known location" in:
        val variation = Snapshot(scope, Inactive, None)
        updateAndGet(scope, variation).unsafeRunSync().map(_.toSnapshot) shouldBe
          Some(Snapshot(scope, Inactive, lastVariation.lastSampledLocation))

      "return a empty last location if the user have never been active" in:
        val neverActiveScope = Scope(UserId.create("greg"), GroupId.create("joy"))
        val variation = Snapshot(neverActiveScope, Inactive, None)
        updateAndGet(neverActiveScope, variation).unsafeRunSync().map(_.toSnapshot) shouldBe
          Some(Snapshot(neverActiveScope, Inactive, None))

    "receiving variations of a routing or SOS user" should:
      "record all variations" in:
        val result = routingVariations.map(updateAndGet(scope, _).unsafeRunSync()).last
        result.map(_.toSnapshot) shouldBe Some(routingVariations.last)
        result.flatMap(_.tracking) shouldBe Some(Tracking(routingVariations.map(_.lastSampledLocation.value)))

  private def updateAndGet(scope: Scope, variation: Session.Snapshot): IO[Option[Session]] =
    storeResource.use: store =>
      for
        _ <- store.update(variation)
        res <- store.sessionOf(scope)
      yield res
