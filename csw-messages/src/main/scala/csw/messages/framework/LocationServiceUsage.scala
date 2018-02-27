package csw.messages.framework

import csw.messages.TMTSerializable
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

/**
 * Describes how a component uses the location service
 */
//TODO: add doc for significance of LocationServiceUsage
sealed abstract class LocationServiceUsage extends EnumEntry with TMTSerializable

object LocationServiceUsage extends Enum[LocationServiceUsage] with PlayJsonEnum[LocationServiceUsage] {

  override def values: immutable.IndexedSeq[LocationServiceUsage] = findValues

  case object DoNotRegister            extends LocationServiceUsage
  case object RegisterOnly             extends LocationServiceUsage
  case object RegisterAndTrackServices extends LocationServiceUsage

  val JDoNotRegister: LocationServiceUsage            = DoNotRegister
  val JRegisterOnly: LocationServiceUsage             = RegisterOnly
  val JRegisterAndTrackServices: LocationServiceUsage = RegisterAndTrackServices
}
