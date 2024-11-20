package io.github.positionpal.location.storage.groups

import scala.concurrent.ExecutionContext

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import cats.effect.kernel.Async
import cats.implicits.toFunctorOps
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.github.positionpal.location.application.groups.UserGroupsStore
import io.github.positionpal.location.commons.CanRaise
import io.github.positionpal.location.domain.{GroupId, UserId}
import io.github.positionpal.location.storage.{StorageUtils, StoreError}

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
      session.executeWrite(insertMemberQuery(groupId, userId)).map(_ => ())

    override def groupsOf(userId: UserId): F[Set[GroupId]] = executeWithErrorHandling:
      session.select(getGroupsQuery(userId))
        .runFold(Set.empty[GroupId])((acc, row) => acc + GroupId(row.getString("GroupId")))

    override def membersOf(groupId: GroupId): F[Set[UserId]] = executeWithErrorHandling:
      session.select(getMembersQuery(groupId))
        .runFold(Set.empty[UserId])((acc, row) => acc + UserId(row.getString("UserId")))

    override def removeMember(groupId: GroupId, userId: UserId): F[Unit] = executeWithErrorHandling:
      session.executeWrite(deleteMemberQuery(groupId, userId)).map(_ => ())

    object Tables:
      val userGroupsByUserId = "UserGroupsByUserId"
      val userGroupsByGroupId = "UserGroupsByGroupId"

    private[CassandraUserGroupsStore] object Queries:
      import Tables.*

      def insertMemberQuery(groupId: GroupId, userId: UserId) =
        batch(insert(userGroupsByUserId)(groupId, userId) :: insert(userGroupsByGroupId)(groupId, userId) :: Nil)

      private def insert(table: String)(groupId: GroupId, userId: UserId): SimpleStatement =
        cql(s"INSERT INTO $keyspace.$table (GroupId, UserId) VALUES (?, ?)", groupId.id, userId.id)

      def getGroupsQuery(userId: UserId) =
        cql(s"SELECT GroupId, UserId FROM $keyspace.$userGroupsByUserId WHERE UserId = ?", userId.id)

      def getMembersQuery(groupId: GroupId) =
        cql(s"SELECT GroupId, UserId FROM $keyspace.$userGroupsByGroupId WHERE GroupId = ?", groupId.id)

      def deleteMemberQuery(groupId: GroupId, userId: UserId) =
        batch(delete(userGroupsByUserId)(groupId, userId) :: delete(userGroupsByGroupId)(groupId, userId) :: Nil)

      private def delete(table: String)(groupId: GroupId, userId: UserId) =
        cql(s"DELETE FROM $keyspace.$table WHERE GroupId = ? AND UserId = ?", groupId.id, userId.id)
