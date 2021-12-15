package csw.location.api

import csw.location.api.client.CswVersion
import csw.location.api.commons.LocationServiceLogger
import csw.location.api.models.Metadata
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix

class CswVersionJvm extends CswVersion {
  private val logger: Logger = LocationServiceLogger.getLogger

  override def check(metadata: Metadata, prefix: Prefix): Unit = {
    validateCswVersion(metadata, prefix)
  }

  private def validateCswVersion(metadata: Metadata, prefix: Prefix): Unit = {
    val mayBeCswVersion = metadata.getCSWVersion
    mayBeCswVersion match {
      case Some(serverCswVersion) =>
        val clientCswVersion = get
        if (serverCswVersion != clientCswVersion)
          logger.error(
            s"csw-version mismatch for Prefix : In $prefix's Metadata Found : $serverCswVersion , Expected : $clientCswVersion"
          )
      case None => logger.error(s"Could not find csw-version for $prefix")
    }
  }
  override def get: String = {
    Option(
      classOf[LocationService].getClass.getPackage.getSpecificationVersion
    ).getOrElse("SNAPSHOT")
  }
}
