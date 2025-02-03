package io.github.positionpal.location.storage.groups

import scala.concurrent.ExecutionContext

import io.github.positionpal.location.storage.{StorageUtils, StoreError}
import akka.actor.typed.ActorSystem
import cats.implicits.toFunctorOps
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import io.github.positionpal.entities.{GroupId, UserId}
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

    override def addMember(groupId: GroupId, userId: UserId): F[Unit] = executeWithErrorHandling:
      session.executeWrite(insertMemberQuery(groupId, userId)).void

    override def groupsOf(userId: UserId): F[Set[GroupId]] = executeWithErrorHandling:
      session
        .select(getGroupsQuery(userId))
        .runFold(Set.empty[GroupId])((acc, row) => acc + GroupId.create(row.getString("GroupId")))

    override def membersOf(groupId: GroupId): F[Set[UserId]] = executeWithErrorHandling:
      session
        .select(getMembersQuery(groupId))
        .runFold(Set.empty[UserId])((acc, row) => acc + UserId.create(row.getString("UserId")))

    override def removeMember(groupId: GroupId, userId: UserId): F[Unit] = executeWithErrorHandling:
      session.executeWrite(deleteMemberQuery(groupId, userId)).void

    object Tables:
      val userGroupsByUserId = "UserGroupsByUserId"
      val userGroupsByGroupId = "UserGroupsByGroupId"

    private[CassandraUserGroupsStore] object Queries:
      import Tables.*

      def insertMemberQuery(groupId: GroupId, userId: UserId) =
        batch(insert(userGroupsByUserId)(groupId, userId) :: insert(userGroupsByGroupId)(groupId, userId) :: Nil)

      private def insert(table: String)(groupId: GroupId, userId: UserId): SimpleStatement =
        cql(s"INSERT INTO $keyspace.$table (GroupId, UserId) VALUES (?, ?)", groupId.value(), userId.value())

      def getGroupsQuery(userId: UserId) =
        cql(s"SELECT GroupId, UserId FROM $keyspace.$userGroupsByUserId WHERE UserId = ?", userId.value())

      def getMembersQuery(groupId: GroupId) =
        cql(s"SELECT GroupId, UserId FROM $keyspace.$userGroupsByGroupId WHERE GroupId = ?", groupId.value())

      def deleteMemberQuery(groupId: GroupId, userId: UserId) =
        batch(delete(userGroupsByUserId)(groupId, userId) :: delete(userGroupsByGroupId)(groupId, userId) :: Nil)

      private def delete(table: String)(groupId: GroupId, userId: UserId) =
        cql(s"DELETE FROM $keyspace.$table WHERE GroupId = ? AND UserId = ?", groupId.value(), userId.value())
