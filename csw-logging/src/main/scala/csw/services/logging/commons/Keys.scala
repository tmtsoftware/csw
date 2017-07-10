package csw.services.logging.commons

import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

import scala.collection.immutable

private[logging] object Keys {

  val CATEGORY       = "@category"
  val SEVERITY       = "@severity"
  val COMPONENT_NAME = "@componentName"
  val TRACE_ID       = "@traceId"
  val MSG            = "@msg"
  val TIMESTAMP      = "timestamp"
  val MESSAGE        = "message"
  val ACTOR          = "actor"
  val FILE           = "file"
  val CLASS          = "class"
  val LINE           = "line"
  val KIND           = "kind"
  val EX             = "ex"
  val HOST           = "@host"
  val SERVICE        = "@service"
  val NAME           = "@name"
  val VERSION        = "@version"
  // File rotation hour of the day (This is considered as UTC Time)
  val FILE_ROTATION_HOUR: Long = 12L

}

private[logging] sealed abstract class Category extends EnumEntry with Lowercase with Serializable {

  def name: String = entryName
}

private[logging] object Category extends Enum[Category] {
  override def values: immutable.IndexedSeq[Category] = findValues

  case object Common extends Category
  case object Gc     extends Category
  case object Time   extends Category
}
