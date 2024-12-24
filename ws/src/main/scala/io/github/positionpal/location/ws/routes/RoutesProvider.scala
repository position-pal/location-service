package io.github.positionpal.location.ws.routes

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route

/** Trait representing an entity that contains the versioned endpoints for a webserver. */
trait RoutesProvider:
  /** A [[String]] representing the version of the routes. */
  def version: String

  /** Return the routes. */
  protected def routes: Route

  /** Return the versioned routes, i.e. the routes prefixed with the version. */
  def versionedRoutes: Route = pathPrefix(version)(routes)
