/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api

import csw.location.api.client.CswVersion
import csw.location.api.commons.LocationServiceLogger
import csw.location.api.models.Metadata
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix

class CswVersionJvm extends CswVersion {
  private val logger: Logger = LocationServiceLogger.getLogger

  override def check(metadata: Metadata, prefix: Prefix): Boolean = {
    val mayBeCswVersion = metadata.getCSWVersion
    mayBeCswVersion match {
      case Some(cswVersionInMetadata) =>
        val myCswVersion = get
        if (cswVersionInMetadata != myCswVersion) {
          logger.error(
            s"csw-version mismatch, In $prefix's Metadata Found:$cswVersionInMetadata, my csw-version:$myCswVersion"
          )
          false
        }
        else true
      case None =>
        logger.error(s"Could not find csw-version for $prefix")
        false
    }
  }

  override def get: String = {
    Option(
      classOf[LocationService].getPackage.getSpecificationVersion
    ).getOrElse("0.1.0-SNAPSHOT")
  }
}
