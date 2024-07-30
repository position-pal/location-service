package io.github.positionpal.location.application.geo

import io.github.positionpal.location.commons.CanRaise
import io.github.positionpal.location.domain.{Address, GPSLocation, Latitude, Longitude}

/** An alias for the map service error. */
type MapsServiceError = String

/** A service to interact with maps and geolocation services.
  * @tparam M the effect constructor type.
  */
trait MapsService[M[_]: CanRaise[MapsServiceError]]:

  /** @return the [[Address]] at the given [[Latitude]] and [[Longitude]]. */
  def addressOf(latitude: Latitude, longitude: Longitude): M[Address]

  /** @return the [[GPSLocation]] of the given [[Address]]. */
  def locationOf(address: Address): M[GPSLocation]
