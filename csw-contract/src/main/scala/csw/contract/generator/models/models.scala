package csw.contract.generator.models

import enumeratum._
import io.bullet.borer.Dom.Element
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.language.implicitConversions
import scala.reflect.ClassTag

case class Endpoint(request: Element, responseType: String, errorTypes: List[String] = Nil)

// single file (e.g., registration.json)
case class ModelType(models: List[Element])

object ModelType {
  import DomHelpers._
  def apply(models: Element*): ModelType                 = new ModelType(models.toList)
  def fromEnum[T <: EnumEntry](enum: Enum[T]): ModelType = new ModelType(enum.values.map(_.entryName).toList.map(encode[String]))
}

case class Service(
    // a file called contract having sections for http and websocket endpoints for this service
    // List("register" -> {request:"",response:"",errors:[""]})
    `http-endpoints`: Map[String, Endpoint],
    `websocket-endpoints`: Map[String, Endpoint],
    // a folder called models for all models for this service
    // Map("registration" -> [])
    models: Map[String, ModelType]
)

case class Services(data: Map[String, Service])

object DomHelpers {
  implicit def encode[T: Encoder: Decoder](x: T): Element           = Json.decode(Json.encode(x).toByteArray).to[Element].value
  implicit def encodeList[T: Encoder: Decoder](x: List[T]): Element = Json.decode(Json.encode(x).toByteArray).to[Element].value
}

object ClassNameHelpers {
  def name[T: ClassTag]: String                = scala.reflect.classTag[T].runtimeClass.getSimpleName
  def arrayName[T: ClassTag]: String           = s"[${name[T]}]"
  def objectName[T <: Singleton](x: T): String = x.toString
}
