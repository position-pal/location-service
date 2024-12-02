package io.github.positionpal.location.storage.sessions

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import io.github.positionpal.location.storage.CassandraConnectionFactory
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserSessionStoreTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike with Matchers:

  import cats.effect.unsafe.implicits.global
  import cats.effect.{IO, Resource}
  import cats.mtl.Handle.handleForApplicativeError
  import io.github.positionpal.entities.UserId
  import io.github.positionpal.location.domain.*
  import io.github.positionpal.location.domain.Session.Snapshot
  import io.github.positionpal.location.domain.UserState.*
  import io.github.positionpal.location.storage.TimeUtils.now

  private val connection = CassandraConnectionFactory[IO](system).get
  private val storeResource = Resource.eval(CassandraUserSessionStore[IO](connection))

  "User Session Database" when:
    "attempting to get the latest updated session of an unknown user" should:
      "return None" in:
        storeResource.use:
          _.sessionOf(UserId.create("unknown-id"))
        .unsafeRunSync() shouldBe None

    val user = UserId.create("u01")
    val initialVariation = Snapshot(user, Active, Some(SampledLocation(now, user, GPSLocation(100.0, 0.0))))
    val lastVariation = Snapshot(user, Active, Some(SampledLocation(now, user, GPSLocation(200.0, 0.0))))
    val routingVariations = Snapshot(user, Routing, Some(SampledLocation(now, user, GPSLocation(300.0, 0.0)))) ::
      Snapshot(user, Routing, Some(SampledLocation(now.plusSeconds(1), user, GPSLocation(400.0, 0.0)))) ::
      Snapshot(user, SOS, Some(SampledLocation(now.plusSeconds(2), user, GPSLocation(500.0, 0.0)))) ::
      Snapshot(user, SOS, Some(SampledLocation(now.plusSeconds(3), user, GPSLocation(600.0, 0.0)))) :: Nil

    "receiving a variation of an active user" should:
      "record their state and last known location" in:
        updateAndGet(user, initialVariation).unsafeRunSync().map(_.toSnapshot) shouldBe Some(initialVariation)

      "update their state if already present" in:
        updateAndGet(user, lastVariation).unsafeRunSync().map(_.toSnapshot) shouldBe Some(lastVariation)

    "receiving a variation of an inactive user" should:
      "record their new state despite leaving inaltered the last known location" in:
        val variation = Snapshot(user, Inactive, None)
        updateAndGet(user, variation).unsafeRunSync().map(_.toSnapshot) shouldBe
          Some(Snapshot(user, Inactive, lastVariation.lastSampledLocation))

      "return a empty last location if the user have never been active" in:
        val neverActiveUser = UserId.create("u02")
        val variation = Snapshot(neverActiveUser, Inactive, None)
        updateAndGet(neverActiveUser, variation).unsafeRunSync().map(_.toSnapshot) shouldBe
          Some(Snapshot(neverActiveUser, Inactive, None))

    "receiving variations of a routing or SOS user" should:
      "record all variations" in:
        val result = routingVariations.map(updateAndGet(user, _).unsafeRunSync()).last
        result.map(_.toSnapshot) shouldBe Some(routingVariations.last)
        result.flatMap(_.tracking) shouldBe Some(Tracking(routingVariations.map(_.lastSampledLocation.value)))

  private def updateAndGet(user: UserId, variation: Session.Snapshot): IO[Option[Session]] =
    storeResource.use: store =>
      for
        _ <- store.update(variation)
        res <- store.sessionOf(user)
      yield res
