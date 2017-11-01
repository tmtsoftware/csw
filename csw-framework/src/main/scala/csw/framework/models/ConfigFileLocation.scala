package csw.framework.models

import csw.messages.TMTSerializable
import enumeratum._

import scala.collection.immutable

/**
 * This is used as indicator for command line applications to fetch configuration files either from local machine or from Configuration service
 */
sealed abstract class ConfigFileLocation extends EnumEntry with TMTSerializable

object ConfigFileLocation extends Enum[ConfigFileLocation] with PlayJsonEnum[ConfigFileLocation] {

  override def values: immutable.IndexedSeq[ConfigFileLocation] = findValues

  case object Local  extends ConfigFileLocation
  case object Remote extends ConfigFileLocation

}
