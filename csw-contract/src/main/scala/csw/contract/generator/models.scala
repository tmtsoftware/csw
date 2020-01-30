package csw.contract.generator

import enumeratum._
import io.bullet.borer.Dom.Element
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.language.implicitConversions
import scala.reflect.ClassTag

case class Endpoint(responseType: Element, errorTypes: List[String] = Nil)

// single file (e.g., registration.json)
case class ModelType(models: List[Element])

object ModelType {
  import DomHelpers._
  def apply(models: Element*): ModelType                    = new ModelType(models.toList)
  def apply[T <: EnumEntry: Enum](enum: Enum[T]): ModelType = new ModelType(enum.values.toList.map(_.entryName))
}

case class Service(
    // a file called contract having sections for http and websocket endpoints for this service
    // List("register" -> {request:"",response:"",errors:[""]})
    `http-endpoints`: Map[String, Endpoint],
    `websocket-endpoints`: Map[String, Endpoint],
    requests: List[Element],
    // a folder called models for all models for this service
    // Map("registration" -> [])
    models: Map[String, ModelType]
)

case class Services(data: Map[String, Service])

object DomHelpers {
  implicit def encode[T: Encoder: Decoder](x: T): Element                  = Json.decode(Json.encode(x).toByteArray).to[Element].value
  implicit def encodeList[T: Encoder: Decoder](xs: List[T]): List[Element] = xs.map(encode[T])
}

object ClassNameHelpers {
  import DomHelpers._
  def name[T: ClassTag]: String                = scala.reflect.classTag[T].runtimeClass.getSimpleName
  def arrayName[T: ClassTag]: Element          = encode(encodeList(List(name[T])))
  def objectName[T <: Singleton](x: T): String = x.toString
}
