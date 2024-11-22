package io.github.positionpal.location.domain

/** The [[io.github.positionpal.location.domain.User]] state information. */
enum UserState:
  /** The user is online and is continuously sending location updates. */
  case Active

  /** The user is not sending location updates. */
  case Inactive

  /** The user requested help. */
  case SOS

  /** The user is currently routing to a destination. */
  case Routing

object UserState:
  extension (userState: UserState)
    /** @return the next state of a user given the [[event]] that occurred. */
    def next(event: DrivingEvent): UserState = event match
      case _: SampledLocation => if userState == Routing || userState == SOS then userState else Active
      case _: RoutingStarted => Routing
      case _: (RoutingStopped | SOSAlertStopped) => Active
      case _: SOSAlertTriggered => SOS
      case _: WentOffline => Inactive
