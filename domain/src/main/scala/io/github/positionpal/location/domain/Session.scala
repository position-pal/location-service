package io.github.positionpal.location.domain

import io.github.positionpal.entities.{GroupId, UserId}

/** Defines the context in which a user's state is visible and relevant to a specific group.
  * A `Scope` captures the idea that a user's state can differ depending on the group
  * they are associated with, enabling group-specific visibility and tracking.
  * @param userId  The unique identifier of the user.
  * @param groupId The unique identifier of the group.
  */
case class Scope(userId: UserId, groupId: GroupId)

/** A facade over the user's state and tracking information. */
trait Session:

  /** The [[Scope]] of this session. */
  def scope: Scope

  /** The current state of the user. */
  def userState: UserState

  /** The last sampled location of the user. */
  def lastSampledLocation: Option[SampledLocation]

  /** The user tracking information. */
  def tracking: Option[Tracking]

  /** @return a new [[Session]] updated according to the given [[event]]. */
  def updateWith(event: DrivingEvent | InternalEvent): Session

object Session:

  import io.github.positionpal.location.domain.UserState.*
  import io.github.positionpal.location.domain.EventConversions.*
  import io.github.positionpal.location.domain.EventConversions.given

  /** A snapshot of the user's state and tracking information. */
  final case class Snapshot(scope: Scope, userState: UserState, lastSampledLocation: Option[SampledLocation])

  extension (s: Session)
    /** @return a [[Snapshot]] from the current [[Session]]. */
    def toSnapshot: Snapshot = Snapshot(s.scope, s.userState, s.lastSampledLocation)

  /** Extractor method for a [[Session]], allowing pattern matching to deconstruct it into its components.
    * @param s The [[Session]] instance to deconstruct.
    * @return an [[Option]]al tuple with the [[Scope]], [[UserState]], [[SampledLocation]] and [[Tracking]] information.
    */
  def unapply(s: Session): Option[(Scope, UserState, Option[SampledLocation], Option[Tracking | MonitorableTracking])] =
    Some((s.scope, s.userState, s.lastSampledLocation, s.tracking))

  /** Creates a new [[Session]] for the given [[groupId]] - [[userId]] initially in the [[Inactive]] state
    * with no tracking information.
    * @param groupId The unique identifier of the group.
    * @param userId The unique identifier of the user.
    */
  def of(groupId: GroupId, userId: UserId): Session = of(Scope(userId, groupId))

  /** Creates a new [[Session]] for the given [[Scope]] initially in the [[Inactive]] state
    * with no tracking information.
    */
  def of(scope: Scope): Session = SessionImpl(scope, userState = Inactive, lastSampledLocation = None, tracking = None)

  /** Creates a new [[Session]] from the given [[groupId]], [[userId]], [[state]],
    * [[lastSample]] and [[tracking]] information.
    */
  def from(
      groupId: GroupId,
      userId: UserId,
      state: UserState,
      lastSample: Option[SampledLocation],
      tracking: Option[Tracking],
  ): Session = from(Scope(userId, groupId), state, lastSample, tracking)

  /** Creates a new [[Session]] from the given [[scope]], [[state]], [[lastSample]] and [[tracking]] information. */
  def from(scope: Scope, state: UserState, lastSample: Option[SampledLocation], tracking: Option[Tracking]): Session =
    SessionImpl(scope, state, lastSample, tracking)

  private case class SessionImpl(
      override val scope: Scope,
      override val userState: UserState,
      override val lastSampledLocation: Option[SampledLocation],
      override val tracking: Option[Tracking],
  ) extends Session:
    override def updateWith(event: DrivingEvent | InternalEvent): Session = event match
      case e: SampledLocation =>
        copy(userState = userState.next(e), tracking = tracking.map(_ + e), lastSampledLocation = Some(e))
      case e: WentOffline =>
        copy(userState = userState.next(e))
      case e: RoutingStarted =>
        copy(userState = userState.next(e), tracking = Some(e.toMonitorableTracking), lastSampledLocation = Some(e))
      case e: SOSAlertTriggered =>
        copy(userState = userState.next(e), tracking = Some(Tracking()), lastSampledLocation = Some(e))
      case e: SOSAlertStopped =>
        copy(userState = userState.next(e), tracking = None)
      case e: RoutingStopped if userState == Routing =>
        copy(userState = userState.next(e), tracking = None)
      case _: StuckAlertTriggered if userState == Routing =>
        copy(tracking = tracking.flatMap(_.asMonitorable).map(_.addAlert(Alert.Stuck)))
      case _: StuckAlertStopped if userState == Routing =>
        copy(tracking = tracking.flatMap(_.asMonitorable).map(_.removeAlert(Alert.Stuck)))
      case _: TimeoutAlertTriggered if userState == Routing =>
        copy(tracking = tracking.flatMap(_.asMonitorable).map(_.addAlert(Alert.Late)))
      case _ => this
