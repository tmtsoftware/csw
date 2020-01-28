package csw.contract.generator.models

import enumeratum._
import io.bullet.borer.Dom.Element
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.language.implicitConversions
import scala.reflect.ClassTag

// single file (e.g., contract.json)
case class Endpoint(request: String, response: String, errors: List[String])

// single file (e.g., registration.json)
case class ModelAdt(models: List[Element])

object ModelAdt {
  import DomHelpers._
  def apply(models: Element*): ModelAdt                 = new ModelAdt(models.toList)
  def fromEnum[T <: EnumEntry](enum: Enum[T]): ModelAdt = new ModelAdt(enum.values.map(_.entryName).toList.map(encode[String]))
}

case class Service(
    // a file called contract for all endpoints for this service
    // List("register" -> {request:"",response:"",errors:[""]})
    endpoints: List[Endpoint],
    // a folder called models for all models for this service
    // Map("registration" -> [])
    models: Map[String, ModelAdt]
)

case class Services(data: Map[String, Service])

object DomHelpers {
  implicit def encode[T: Encoder: Decoder](x: T): Element           = Json.decode(Json.encode(x).toByteArray).to[Element].value
  implicit def encodeList[T: Encoder: Decoder](x: List[T]): Element = Json.decode(Json.encode(x).toByteArray).to[Element].value
}

object ClassNameHelpers {
  def name[T: ClassTag]: String       = scala.reflect.classTag[T].runtimeClass.getSimpleName
  def listName[T: ClassTag]: String   = s"List[${name[T]}]"
  def optionName[T: ClassTag]: String = s"Option[${name[T]}]"
}
