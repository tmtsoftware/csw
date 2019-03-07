package csw.time.scheduler
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import csw.time.core.models.TMTTime

/**
 * This API allows users to get a representation of [[csw.time.core.models.TMTTime]] in a specific Time Zone,
 * returned as a [[java.time.ZonedDateTime]].
 */
object TMTTimeHelper {

  /**
   * Combines the [[csw.time.core.models.TMTTime]] with the given timezone to get a [[java.time.ZonedDateTime]]
   *
   * @param zoneId id of the required zone
   * @return time at the given zone
   */
  def atZone(tmtTime: TMTTime, zoneId: ZoneId): ZonedDateTime = tmtTime.value.atZone(zoneId)

  /**
   * Combines the [[csw.time.core.models.TMTTime]] with the Local timezone to get a [[java.time.ZonedDateTime]].
   * Local timezone is the system's default timezone.
   *
   * @return time at the Local zone
   */
  def atLocal(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneId.systemDefault())

  /**
   * Combines the [[csw.time.core.models.TMTTime]] with the Hawaii timezone to get a [[java.time.ZonedDateTime]].
   *
   * @return time at the Hawaii-Aleutian Standard Time (HST) zone
   */
  def atHawaii(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneId.of("US/Hawaii"))

  /**
   * Converts the [[csw.time.core.models.TMTTime]] instance to [[java.time.ZonedDateTime]] by adding 0 offset of UTC timezone.
   *
   * @return zoned representation of the TMTTime
   */
  def toZonedDateTime(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneOffset.UTC)
}
