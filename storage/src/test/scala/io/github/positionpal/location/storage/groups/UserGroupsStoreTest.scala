package io.github.positionpal.location.storage.groups

import io.github.positionpal.location.storage.CassandraConnectionFactory
import cats.mtl.Handle.handleForApplicativeError
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.entities.{GroupId, User, UserId}
import org.scalatest.wordspec.AnyWordSpecLike
import cats.effect.{IO, Resource}

class UserGroupsStoreTest extends ScalaTestWithActorTestKit() with AnyWordSpecLike with Matchers:

  import cats.effect.unsafe.implicits.global

  private def uid(id: String) = UserId.create(id)
  private val luke = User.create(uid("206741bb-aba7-41b5-b29c-955538c889e1"), "Luke", "Skywalker", "sky.luke@gmail.com")
  private val greg = User.create(uid("94c22478-de1d-46d8-a096-2d820f2a255a"), "Greg", "Johnson", "greg.johns@gmail.com")
  private val josh = User.create(uid("11c83e68-e967-4aa0-9ea7-947c6c5ad080"), "Josh", "Smith", "josh.smith@gmail.com")
  private val lily = User.create(uid("4c4c5539-57fd-4093-98e0-16f5106009a6"), "Lily", "Johnson", "lily.johns@gmail.com")
  private val groups = Map(
    GroupId.create("astro") -> Set(luke, greg),
    GroupId.create("divine") -> Set(luke, josh, lily),
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
            storeResource.use(_.groupsOf(member.id())).unsafeRunSync() shouldBe expectedGroups

    "removing a user from a group" should:
      "delete the record correctly" in:
        val targetGroup = groups.head
        targetGroup._2.foreach: member =>
          storeResource.use(_.removeMember(targetGroup._1, member.id())).unsafeRunSync()
        storeResource.use(_.membersOf(targetGroup._1)).unsafeRunSync() shouldBe Set.empty
