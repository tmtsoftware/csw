package csw.contract.generator

import enumeratum._
import io.bullet.borer.derivation.CompactMapBasedCodecs
import io.bullet.borer.{Encoder, Writer}

import scala.reflect.ClassTag

case class Endpoint(requestType: String, responseType: String, errorTypes: List[String] = Nil)

case class ModelType[T: Encoder](models: List[T]) {
  implicit def enc: Encoder[ModelType[T]] = CompactMapBasedCodecs.deriveEncoder
  def write(w: Writer): w.type            = w.write(this)
}

object ModelType {
  def apply[T: Encoder](models: T*): ModelType[T]                   = new ModelType(models.toList)
  def apply[T <: EnumEntry: Enum](enum: Enum[T]): ModelType[String] = new ModelType(enum.values.toList.map(_.entryName))
}

case class Contract(endpoints: List[Endpoint], requests: Map[String, ModelType[_]])

case class Service(
    `http-contract`: Contract,
    `websocket-contract`: Contract,
    models: Map[String, ModelType[_]]
)

case class Services(data: Map[String, Service])

object ClassNameHelpers {
  def name[T: ClassTag]: String                = scala.reflect.classTag[T].runtimeClass.getSimpleName
  def arrayName[T: ClassTag]: String           = s"[${name[T]}]"
  def objectName[T <: Singleton](x: T): String = x.toString
}
