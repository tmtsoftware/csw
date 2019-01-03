package csw.clock.natives.models

sealed trait OSType

object OSType {
  case object Linux extends OSType
  case object Other extends OSType

  val value: OSType = Other
}
