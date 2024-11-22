package io.github.positionpal.location.domain

import io.github.positionpal.entities.UserId

/** A facade over the user's state and tracking information. */
trait Session:

  /** The identifier of the user. */
  def userId: UserId

  /** The current state of the user. */
  def userState: UserState

  /** The last sampled location of the user. */
  def lastSampledLocation: Option[SampledLocation]

  /** The user tracking information. */
  def tracking: Option[Tracking | MonitorableTracking]

  /** @return a new [[Session]] updated according to the given [[event]]. */
  def updateWith(event: DrivingEvent): Session

object Session:

  import io.github.positionpal.location.domain.UserState.*
  import io.github.positionpal.location.domain.EventConversions.*
  import io.github.positionpal.location.domain.EventConversions.given

  /** A snapshot of the user's state and tracking information. */
  final case class Snapshot(userId: UserId, userState: UserState, lastSampledLocation: Option[SampledLocation])

  extension (s: Session)
    /** @return a [[Snapshot]] from the current [[Session]]. */
    def toSnapshot: Snapshot = Snapshot(s.userId, s.userState, s.lastSampledLocation)

  def unapply(
      s: Session,
  ): Option[(UserId, UserState, Option[SampledLocation], Option[Tracking | MonitorableTracking])] =
    Some((s.userId, s.userState, s.lastSampledLocation, s.tracking))

  /** Creates a new [[Session]] for the given [[userId]],
    * initially in the [[Inactive]] state with no tracking information.
    */
  def of(userId: UserId): Session =
    SessionImpl(userId, userState = Inactive, lastSampledLocation = None, tracking = None)

  /** Creates a new [[Session]] from the given [[userId]], [[state]], [[lastSample]] and [[tracking]] information. */
  def from(userId: UserId, state: UserState, lastSample: Option[SampledLocation], tracking: Option[Tracking]): Session =
    SessionImpl(userId, state, lastSample, tracking)

  private case class SessionImpl(
      override val userId: UserId,
      override val userState: UserState,
      override val lastSampledLocation: Option[SampledLocation],
      override val tracking: Option[Tracking | MonitorableTracking],
  ) extends Session:
    override def updateWith(event: DrivingEvent): Session = event match
      case e: SampledLocation =>
        copy(userState = userState.next(e), tracking = tracking.map(_ + e), lastSampledLocation = Some(e))
      case e: RoutingStarted =>
        copy(userState = userState.next(e), tracking = Some(e.toMonitorableTracking), lastSampledLocation = Some(e))
      case e: SOSAlertTriggered =>
        copy(userState = userState.next(e), tracking = Some(Tracking()), lastSampledLocation = Some(e))
      case e: (SOSAlertStopped | RoutingStopped) => copy(userState = userState.next(e), tracking = None)
      case e: WentOffline => copy(userState = userState.next(e))
