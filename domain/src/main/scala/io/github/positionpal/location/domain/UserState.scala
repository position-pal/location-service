package io.github.positionpal.location.domain

import io.github.positionpal.location.domain.Alert.Late

/** The user state information. */
enum UserState:
  /** The user is online and is continuously sending location updates. */
  case Active

  /** The user is not sending location updates. */
  case Inactive

  /** The user requested help. */
  case SOS

  /** The user is actively routing to a destination. */
  case Routing

  /** User behavior flagged as suspicious by system monitoring, requiring review. No help request detected. */
  case Warning

/** An invalid state, reached because of an unexpected event.
  * @param reason The reason why the state is invalid.
  */
case class InvalidState(reason: String)

object UserState:

  extension (current: UserState)
    /** @return the next state of a user given the [[event]] that occurred. */
    def next(event: DrivingEvent, tracking: Option[Tracking]): Either[InvalidState, UserState] =
      event match
        case _: SOSAlertTriggered => current ~> SOS unlessIsIn SOS
        case _: SOSAlertStopped => current ~> Active onlyIfIn SOS
        case _: RoutingStarted => current ~> Routing onlyIfIn (Active, Inactive)
        case _: RoutingStopped => current ~> Active onlyIfIn (Routing, Warning)
        case _: (TimeoutAlertTriggered | StuckAlertTriggered) => current ~> Warning onlyIfIn (Routing, Warning)
        case _: StuckAlertStopped => current ~> Routing onlyIfIn Warning
        case _: SampledLocation =>
          current match
            case Inactive => current ~> Active
            case Warning if !(tracking flatMap (_.asMonitorable) exists (_ has Late)) => current ~> Routing
            case _ => stay
        case _: WentOffline =>
          current match
            case SOS => stay
            case Routing | Warning => current ~> Warning
            case _ => current ~> Inactive unlessIsIn Inactive

    infix private def stay: Transition = current ~> current

    infix private def ~>(next: UserState): Transition = Transition(current, next)

  private case class Transition(current: UserState, next: UserState):
    infix def unlessIsIn(states: UserState*): Either[InvalidState, UserState] =
      validateState(!states.contains(current), s"User is already in ${states.mkString(" or ")} state")

    infix def onlyIfIn(states: UserState*): Either[InvalidState, UserState] =
      validateState(states.contains(current), s"User is not in ${states.mkString(" nor ")} state.")

    private def validateState(cond: Boolean, reason: String) = Either.cond(cond, next, InvalidState(reason))

  private given Conversion[Transition, Either[InvalidState, UserState]] = trn => Right(trn.next)
