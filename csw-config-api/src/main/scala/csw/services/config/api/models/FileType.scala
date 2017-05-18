package csw.services.config.api.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
 * Represents the type of storage for a configuration file.
 */
sealed abstract class FileType extends EnumEntry with Serializable

object FileType extends Enum[FileType] {
  override def values: immutable.IndexedSeq[FileType] = findValues

  /**
   * Represents a file to be stored in annex store
   */
  case object Annex extends FileType

  /**
   * Represents a file to be stored in the repository normally
   */
  case object Normal extends FileType

  /**
   * comma separated string representation of enum values
   */
  val stringify: String = values.mkString(",")
}
