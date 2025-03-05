package io.github.positionpal.location.domain

object GeoUtils:
  val bolognaCampus: Address = Address(
    "Viale del Risorgimento, 2, 40136 Bologna BO",
    GPSLocation(latitude = 44.487912, longitude = 11.32885),
  )
  val imolaCampus: Address = Address(
    "Via Giuseppe Garibaldi, 24, Imola",
    GPSLocation(latitude = 44.352962, longitude = 11.711285),
  )
  val forliCampus: Address = Address(
    "Padiglione Morgagni, Via Giacomo della Torre, 1, Forlì",
    GPSLocation(latitude = 44.219119, longitude = 12.042589),
  )
  val cesenaCampus: Address = Address(
    "Via Dell'Università, 50, Cesena",
    GPSLocation(latitude = 44.1476299926484, longitude = 12.2357184467018),
  )
  val riminiCampus: Address = Address(
    "Corso d'Augusto, 237, Rimini",
    GPSLocation(latitude = 44.062709, longitude = 12.564768),
  )
  val ravennaCampus: Address = Address(
    "Via Alfredo Baccarini, 27, Ravenna",
    GPSLocation(latitude = 44.412721, longitude = 12.200321),
  )
end GeoUtils
