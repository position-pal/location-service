package io.github.positionpal.location.storage.groups

import scala.concurrent.ExecutionContext

import io.github.positionpal.location.storage.{StorageUtils, StoreError}
import akka.actor.typed.ActorSystem
import cats.implicits.toFunctorOps
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import io.github.positionpal.entities.{GroupId, User, UserId}
import cats.effect.kernel.Async
import io.github.positionpal.location.application.groups.UserGroupsStore
import io.github.positionpal.location.commons.CanRaise
import com.datastax.oss.driver.api.core.cql.SimpleStatement

object CassandraUserGroupsStore:

  def apply[F[_]: Async: CanRaise[StoreError]](
      session: F[CassandraSession],
      keyspace: String = "locationservice",
  )(using actorSystem: ActorSystem[?]): F[UserGroupsStore[F, Unit]] = session.map(Impl(_, keyspace))

  private class Impl[F[_]: Async: CanRaise[StoreError]](using actorSystem: ActorSystem[?])(
      session: CassandraSession,
      keyspace: String,
  ) extends UserGroupsStore[F, Unit]
      with StorageUtils:
    import Queries.*

    given ExecutionContext = actorSystem.executionContext

    override def addMember(groupId: GroupId, user: User): F[Unit] = executeWithErrorHandling:
      session.executeWrite(insertMemberQuery(groupId, user)).void

    override def groupsOf(userId: UserId): F[Set[GroupId]] = executeWithErrorHandling:
      session
        .select(getGroupsQuery(userId))
        .runFold(Set.empty)((acc, row) => acc + GroupId.create(row.getString("GroupId")))

    override def membersOf(groupId: GroupId): F[Set[User]] = executeWithErrorHandling:
      session
        .select(getMembersQuery(groupId))
        .runFold(Set.empty)((acc, row) =>
          acc + User.create(
            UserId.create(row.getString("UserId")),
            row.getString("Name"),
            row.getString("Surname"),
            row.getString("Email"),
          ),
        )

    override def removeMember(groupId: GroupId, userId: UserId): F[Unit] = executeWithErrorHandling:
      session.executeWrite(deleteMemberQuery(groupId, userId)).void

    object Tables:
      val userGroupsByUserId = "UserGroupsByUserId"
      val userGroupsByGroupId = "UserGroupsByGroupId"

    private[CassandraUserGroupsStore] object Queries:
      import Tables.*

      def insertMemberQuery(groupId: GroupId, user: User) =
        batch(insert(userGroupsByUserId)(groupId, user) :: insert(userGroupsByGroupId)(groupId, user) :: Nil)

      private def insert(table: String)(groupId: GroupId, user: User) = cql(
        s"INSERT INTO $keyspace.$table (GroupId, UserId, Name, Surname, Email) VALUES (?, ?, ?, ?, ?)",
        groupId.value(),
        user.id().value(),
        user.name(),
        user.surname(),
        user.email(),
      )

      def getGroupsQuery(userId: UserId) =
        cql(s"SELECT GroupId FROM $keyspace.$userGroupsByUserId WHERE UserId = ?", userId.value())

      def getMembersQuery(groupId: GroupId) = cql(
        s"SELECT UserId, Name, Surname, Email FROM $keyspace.$userGroupsByGroupId WHERE GroupId = ?",
        groupId.value(),
      )

      def deleteMemberQuery(groupId: GroupId, userId: UserId) =
        batch(delete(userGroupsByUserId)(groupId, userId) :: delete(userGroupsByGroupId)(groupId, userId) :: Nil)

      private def delete(table: String)(groupId: GroupId, userId: UserId) =
        cql(s"DELETE FROM $keyspace.$table WHERE GroupId = ? AND UserId = ?", groupId.value(), userId.value())
