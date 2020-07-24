package csw.contract.generator

import csw.prefix.codecs.CommonCodecs
import enumeratum._
import io.bullet.borer.{Codec, Decoder, Encoder, Writer}

import scala.reflect.ClassTag

class ModelType[T: Encoder: Decoder: ClassTag] private (val models: List[T]) {
  def codec: Codec[T]          = Codec.of[T]
  def write(w: Writer): w.type = w.write(models)
  def name: String             = scala.reflect.classTag[T].runtimeClass.getSimpleName.stripSuffix("$")
}

object ModelType extends CommonCodecs {
  def apply[T: Encoder: Decoder: ClassTag](models: T*): ModelType[T]      = new ModelType(models.toList)
  def apply[T: Encoder: Decoder: ClassTag](models: List[T]): ModelType[T] = new ModelType(models)
  def apply[T <: EnumEntry: Enum: ClassTag](enum: Enum[T]): ModelType[T]  = new ModelType(enum.values.toList)
}

class ModelSet private (val modelTypes: List[ModelType[_]])

object ModelSet {
  def models(modelTypes: ModelType[_]*): ModelSet      = new ModelSet(modelTypes.toList)
  def requests[T](modelTypes: ModelType[T]*): ModelSet = new ModelSet(modelTypes.toList)
}

case class Endpoint(requestType: String, responseType: String, errorTypes: List[String] = Nil, description: Option[String] = None)

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
