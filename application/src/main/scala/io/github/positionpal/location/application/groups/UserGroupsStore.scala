package io.github.positionpal.location.application.groups

import io.github.positionpal.location.domain.{GroupId, UserId}

/** The reading model projection for groups. It encapsulates the read-side
  * operations for querying and retrieving user groups related information from the underlying store.
  * @tparam F the effect type
  */
trait UserGroupsReader[F[_]]:

  /** @return the groups of the given [[userId]]. */
  def groupsOf(userId: UserId): F[Set[GroupId]]

  /** @return the members of the given [[groupId]]. */
  def membersOf(groupId: GroupId): F[Set[UserId]]

/** The writing model projection for groups. It encapsulates the write-side
  * operations for saving user groups related information to the underlying store.
  * @tparam F the effect type
  * @tparam T the result type of the write operation
  */
trait UserGroupsWriter[F[_], T]:

  /** Add the given [[userId]] to the given [[groupId]]. */
  def addMember(groupId: GroupId, userId: UserId): F[T]

  /** Remove the given [[userId]] from the given [[groupId]]. */
  def removeMember(groupId: GroupId, userId: UserId): F[T]

/** A store for user groups related information, supporting both read and write operations. */
trait UserGroupsStore[F[_], T] extends UserGroupsReader[F] with UserGroupsWriter[F, T]
