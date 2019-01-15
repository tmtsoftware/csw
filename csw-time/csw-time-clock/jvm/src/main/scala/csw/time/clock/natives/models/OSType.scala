package csw.time.clock.natives.models

import java.util.Locale

private[time] sealed trait OSType

private[time] object OSType {
  case object Linux extends OSType
  case object Other extends OSType

  val value: OSType = {
    val OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
    if (OS.indexOf("nux") >= 0) Linux else Other
  }
}
