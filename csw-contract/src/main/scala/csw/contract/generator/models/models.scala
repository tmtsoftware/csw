package csw.contract.generator.models

import enumeratum._
import io.bullet.borer.Dom.Element
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.language.implicitConversions

// single file (e.g., register.json)
case class Endpoint(requests: List[Element], responses: List[Element])

// single file (e.g., registration.json)
case class ModelAdt(models: List[Element])

object ModelAdt {
  import DomHelpers._
  def apply(models: Element*): ModelAdt                 = new ModelAdt(models.toList)
  def fromEnum[T <: EnumEntry](enum: Enum[T]): ModelAdt = new ModelAdt(enum.values.map(_.entryName).toList.map(encode[String]))
}

case class Service(
    // a folder called endpoints for all endpoints for this service
    // Map("register" -> {})
    endpoints: Map[String, Endpoint],
    // a folder called models for all models for this service
    // Map("registration" -> [])
    models: Map[String, ModelAdt]
)

case class Services(data: Map[String, Service])

object DomHelpers {
  implicit def encode[T: Encoder: Decoder](x: T): Element           = Json.decode(Json.encode(x).toByteArray).to[Element].value
  implicit def encodeList[T: Encoder: Decoder](x: List[T]): Element = Json.decode(Json.encode(x).toByteArray).to[Element].value
}
