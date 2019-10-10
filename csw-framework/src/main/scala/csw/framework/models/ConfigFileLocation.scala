package csw.framework.models

import enumeratum._

import scala.collection.immutable

/**
 * This is used as indicator for command line applications to fetch configuration files either from local machine or from Configuration service
 */
private[csw] sealed abstract class ConfigFileLocation extends EnumEntry

private[csw] object ConfigFileLocation extends Enum[ConfigFileLocation] {

  override def values: immutable.IndexedSeq[ConfigFileLocation] = findValues

  case object Local  extends ConfigFileLocation
  case object Remote extends ConfigFileLocation

}
