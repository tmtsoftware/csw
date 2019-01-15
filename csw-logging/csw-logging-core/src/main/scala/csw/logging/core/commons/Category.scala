package csw.logging.core.commons

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

private[csw] sealed abstract class Category extends EnumEntry with Lowercase with Serializable {
  def name: String = entryName
}

private[csw] object Category extends Enum[Category] {
  override def values: immutable.IndexedSeq[Category] = findValues

  case object Common extends Category
  case object Gc     extends Category
  case object Time   extends Category
}
