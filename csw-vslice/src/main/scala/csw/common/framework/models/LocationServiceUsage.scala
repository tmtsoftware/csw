package csw.common.framework.models

import enumeratum.{Enum, EnumEntry}
import spray.json.{JsString, JsValue, JsonFormat}

import scala.collection.immutable

/**
 * Describes how a component uses the location service
 */
sealed abstract class LocationServiceUsage extends EnumEntry with Serializable

object LocationServiceUsage extends Enum[LocationServiceUsage] {

  import csw.param.formats.JsonSupport._

  override def values: immutable.IndexedSeq[LocationServiceUsage] = findValues
  implicit val format: JsonFormat[LocationServiceUsage]           = enumFormat(this)

  def enumFormat[T <: EnumEntry](enum: Enum[T]): JsonFormat[T] = new JsonFormat[T] {
    override def write(obj: T): JsValue = JsString(obj.entryName)
    override def read(json: JsValue): T = enum.withName(json.convertTo[String])
  }

  case object DoNotRegister            extends LocationServiceUsage
  case object RegisterOnly             extends LocationServiceUsage
  case object RegisterAndTrackServices extends LocationServiceUsage
}
