package csw.services.config.api.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class FileType extends EnumEntry with Serializable

object FileType extends Enum[FileType] {
  override def values: immutable.IndexedSeq[FileType] = findValues
  case object Annex  extends FileType
  case object Normal extends FileType

  val stringify: String = values.mkString(",")
}
