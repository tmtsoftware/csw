package csw.contract.generator

import csw.prefix.codecs.CommonCodecs
import enumeratum._
import io.bullet.borer.{Encoder, Writer}

import scala.reflect.ClassTag

case class Endpoint(requestType: String, responseType: String, errorTypes: List[String] = Nil, description: Option[String] = None)

class ModelType[T: Encoder: ClassTag] private (val models: List[T]) {
  implicit def enc: Encoder[ModelType[T]] = Encoder[List[T]].contramap(_.models)
  def write(w: Writer): w.type            = w.write(this)
  def name: String                        = scala.reflect.classTag[T].runtimeClass.getSimpleName.stripSuffix("$")
}

case class ModelSet(modelTypes: List[ModelType[_]])
object ModelSet {
  def apply(modelTypes: ModelType[_]*): ModelSet = new ModelSet(modelTypes.toList)
}

object ModelType extends CommonCodecs {
  def apply[T: Encoder: ClassTag](models: T*): ModelType[T]              = new ModelType(models.toList)
  def apply[T: Encoder: ClassTag](models: List[T]): ModelType[T]         = new ModelType(models)
  def apply[T <: EnumEntry: Enum: ClassTag](enum: Enum[T]): ModelType[T] = new ModelType(enum.values.toList)
}

case class Contract(endpoints: List[Endpoint], requests: ModelSet)

case class Service(
    `http-contract`: Contract,
    `websocket-contract`: Contract,
    models: ModelSet,
    readme: Readme
)

case class Readme(content: String)

case class Services(data: Map[String, Service])

object ClassNameHelpers {
  def name[T: ClassTag]: String                = scala.reflect.classTag[T].runtimeClass.getSimpleName
  def arrayName[T: ClassTag]: String           = s"[${name[T]}]"
  def objectName[T <: Singleton](x: T): String = x.toString
}
