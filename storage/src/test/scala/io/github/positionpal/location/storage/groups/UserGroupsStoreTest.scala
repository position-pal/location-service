package io.github.positionpal.location.storage.groups

import io.github.positionpal.location.storage.CassandraConnectionFactory
import cats.mtl.Handle.handleForApplicativeError
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.entities.{GroupId, UserId}
import org.scalatest.wordspec.AnyWordSpecLike
import cats.effect.{IO, Resource}

class UserGroupsStoreTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike with Matchers:

  import cats.effect.unsafe.implicits.global

  private val groups = Map(
    GroupId.create("astro") -> Set(UserId.create("Luca"), UserId.create("Greg")),
    GroupId.create("divine") -> Set(UserId.create("Luca"), UserId.create("Josh"), UserId.create("Alice")),
  )
  private val connection = CassandraConnectionFactory[IO](system).get
  private val storeResource = Resource.eval(CassandraUserGroupsStore[IO](connection))

  "UserGroupsStore" when:
    "attempting to add members to a group" should:
      "create successfully a new record" in:
        groups.map: (group, members) =>
          members.map: member =>
            val result = storeResource.use(_.addMember(group, member)).attempt.unsafeRunSync()
            result shouldBe Right(())

    "attempting to get all members of a group" should:
      "work seamlessly" in:
        groups.keys.foreach: group =>
          val result = storeResource.use(_.membersOf(group)).unsafeRunSync()
          result shouldBe groups(group)

    "attempting to get all groups of a user" should:
      "work seamlessly" in:
        groups.foreach: (_, members) =>
          members.foreach: member =>
            val expectedGroups = groups.filter(_._2.contains(member)).keys.toSet
            storeResource.use(_.groupsOf(member)).unsafeRunSync() shouldBe expectedGroups

    "removing a user from a group" should:
      "delete the record correctly" in:
        val targetGroup = groups.head
        targetGroup._2.foreach: member =>
          storeResource.use(_.removeMember(targetGroup._1, member)).unsafeRunSync()
        storeResource.use(_.membersOf(targetGroup._1)).unsafeRunSync() shouldBe Set.empty
