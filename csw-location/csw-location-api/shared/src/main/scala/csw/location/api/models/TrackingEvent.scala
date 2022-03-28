/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.models

import csw.location.api.codec.LocationSerializable

/**
 * TrackingEvent is used to represent location events while tracking the connection
 */
sealed abstract class TrackingEvent extends LocationSerializable {

  /**
   * The connection for which this TrackingEvent is created
   */
  def connection: Connection
}

/**
 * This event represents modification in location details
 *
 * @param location the updated location for the tracked connection
 */
case class LocationUpdated(location: Location) extends TrackingEvent {

  /**
   * The connection for which this TrackingEvent is created
   */
  override def connection: Connection = location.connection
}

/**
 * This event represents unavailability of a location
 *
 * @param connection for which the location no longer exists
 */
case class LocationRemoved(connection: Connection) extends TrackingEvent
