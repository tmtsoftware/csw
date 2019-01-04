package csw.time.client
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import csw.time.api.models.TMTTime

object TMTTimeHelper {

  /**
   * Combines the [[csw.time.api.models.UTCTime]] with the given timezone to get a [[java.time.ZonedDateTime]]
   *
   * @param zoneId id of the required zone
   * @return time atZone the given zone
   */
  def atZone(tmtTime: TMTTime, zoneId: ZoneId): ZonedDateTime = tmtTime.value.atZone(zoneId)

  /**
   * Combines the [[csw.time.api.models.UTCTime]] with the Local timezone to get a [[java.time.ZonedDateTime]]. Local timezone is the system's default timezone.
   *
   * @return time atZone the Local zone
   */
  def atLocal(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneId.systemDefault())

  /**
   * Combines the [[csw.time.api.models.UTCTime]] with the Hawaii timezone to get a [[java.time.ZonedDateTime]].
   *
   * @return time atZone the Hawaii-Aleutian Standard Time (HST) zone
   */
  def atHawaii(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneId.of("US/Hawaii"))

  /**
   * Converts the [[csw.time.api.models.UTCTime]] instance to [[java.time.ZonedDateTime]] by adding 0 offset of UTC.
   *
   * @return zoned representation of the UTCTime
   */
  def toZonedDateTime(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneOffset.UTC)
}
