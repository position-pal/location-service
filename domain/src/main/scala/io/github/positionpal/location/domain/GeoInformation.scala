package io.github.positionpal.location.domain

/** The latitude of a [[GPSLocation]]. */
type Latitude = Double

/** The longitude of a [[GPSLocation]]. */
type Longitude = Double

/** A GPS location, identified by a [[Latitude]] and a [[Longitude]]. */
final case class GPSLocation(latitude: Latitude, longitude: Longitude)

/** An address, identified by a [[name]] and a [[position]]. */
final case class Address(name: String, position: GPSLocation)
