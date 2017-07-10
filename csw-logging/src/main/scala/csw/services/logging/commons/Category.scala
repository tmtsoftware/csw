package csw.services.logging.commons

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

private[logging] sealed abstract class Category extends EnumEntry with Lowercase with Serializable {
  def name: String = entryName
}

private[logging] object Category extends Enum[Category] {
  override def values: immutable.IndexedSeq[Category] = findValues

  case object Common extends Category
  case object Gc     extends Category
  case object Time   extends Category
}
