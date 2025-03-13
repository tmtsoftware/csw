/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.scaladsl

import org.apache.pekko.Done
import csw.location.api.models.{Connection, Location}

import scala.concurrent.Future

/**
 * RegistrationResult represents successful registration of a location
 */
trait RegistrationResult {

  /**
   * The successful registration of location can be unregistered using this method
   *
   * @note this method is idempotent, which means multiple call to unregister the same connection will be no-op once successfully
   *       unregistered from location service
   * @return a future which completes when un-registrstion is done successfully or fails with
   *         [[csw.location.api.exceptions.UnregistrationFailed]]
   */
  def unregister(): Future[Done]

  /**
   * The `unregister` method will use the connection of this location to unregister from `LocationService`
   *
   * @return the handle to the `Location` that got registered in `LocationService`
   */
  def location: Location
}

object RegistrationResult {
  def from(_location: Location, _unregister: Connection => Future[Done]): RegistrationResult =
    new RegistrationResult {
      override def unregister(): Future[Done] = _unregister(location.connection)
      override def location: Location         = _location
    }
}
