package csw.contract.generator

import enumeratum._
import io.bullet.borer.Dom.Element
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.language.implicitConversions
import scala.reflect.ClassTag

case class Endpoint(requestType: String, responseType: Element, errorTypes: List[String] = Nil)

case class ModelType private (models: List[Element])

object ModelType {
  import DomHelpers._
  def apply(models: Element*): ModelType                    = new ModelType(models.toList)
  def apply[T <: EnumEntry: Enum](enum: Enum[T]): ModelType = new ModelType(enum.values.toList.map(_.entryName))
}

case class Contract(endpoints: List[Endpoint], requests: Map[String, ModelType])

case class Service(
    `http-contract`: Contract,
    `websocket-contract`: Contract,
    models: Map[String, ModelType]
)

case class Services(data: Map[String, Service])

object DomHelpers {
  implicit def encode[T: Encoder: Decoder](x: T): Element = Json.decode(Json.encode(x).toByteArray).to[Element].value
}

object ClassNameHelpers {
  import DomHelpers._
  def name[T: ClassTag]: String                = scala.reflect.classTag[T].runtimeClass.getSimpleName
  def arrayName[T: ClassTag]: Element          = encode(List(name[T]))
  def objectName[T <: Singleton](x: T): String = x.toString
}
