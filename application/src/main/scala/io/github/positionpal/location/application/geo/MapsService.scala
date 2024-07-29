package io.github.positionpal.location.application.geo

import io.github.positionpal.location.domain.{Address, GPSLocation, Latitude, Longitude}

/** A service to interact with maps and geolocation services.
  * @tparam M the effect constructor type.
  */
trait MapsService[M[_]]:

  /** @return the [[Address]] at the given [[Latitude]] and [[Longitude]]. */
  def addressOf(latitude: Latitude, longitude: Longitude): M[Option[Address]]

  /** @return the [[GPSLocation]] of the given [[Address]]. */
  def locationOf(address: Address): M[Option[GPSLocation]]
