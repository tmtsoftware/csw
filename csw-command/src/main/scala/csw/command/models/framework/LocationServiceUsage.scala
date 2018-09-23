package csw.command.models.framework
import csw.serializable.TMTSerializable
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

/**
 * Describes what action to take for a component on its boot-up regarding its registration with location service. This information
 * is read from the config file for the component and used by `csw-framework` while spawning it.
 */
sealed abstract class LocationServiceUsage extends EnumEntry with TMTSerializable

object LocationServiceUsage extends Enum[LocationServiceUsage] with PlayJsonEnum[LocationServiceUsage] {

  override def values: immutable.IndexedSeq[LocationServiceUsage] = findValues

  /**
   * Represents the action to skip registration with location service
   */
  case object DoNotRegister extends LocationServiceUsage

  /**
   * Represents the action to register with location service. Mostly this is used by HCDs.
   */
  case object RegisterOnly extends LocationServiceUsage

  /**
   * Represents the action to register with location service and track other components. Mostly this is used by
   * Sequencers and Assemblies.
   */
  case object RegisterAndTrackServices extends LocationServiceUsage

  /**
   * A Java helper representing DoNotRegister action
   */
  val JDoNotRegister: LocationServiceUsage = DoNotRegister

  /**
   * A Java helper representing RegisterOnly action
   */
  val JRegisterOnly: LocationServiceUsage = RegisterOnly

  /**
   * A Java helper representing RegisterAndTrackServices action
   */
  val JRegisterAndTrackServices: LocationServiceUsage = RegisterAndTrackServices
}
