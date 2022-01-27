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
  def apply[T: Encoder: Decoder: ClassTag](models: T*): ModelType[T]       = new ModelType(models.toList)
  def apply[T: Encoder: Decoder: ClassTag](models: List[T]): ModelType[T]  = new ModelType(models)
  def apply[T <: EnumEntry: Enum: ClassTag](`enum`: Enum[T]): ModelType[T] = new ModelType(`enum`.values.toList)
}

class ModelSet private (val modelTypes: List[ModelType[_]])

object ModelSet {
  def models(modelTypes: ModelType[_]*): ModelSet = new ModelSet(modelTypes.toList)
}

class RequestSet[T: Encoder: Decoder] {
  private var requests: List[ModelType[_]] = Nil

  protected def requestType[R <: T: ClassTag](models: R*): Unit = {
    implicit val codec: Codec[R] = Codec.of[T].asInstanceOf[Codec[R]]
    requests ::= ModelType(models.toList)
  }

  def modelSet: ModelSet = ModelSet.models(requests.reverse: _*)
}

case class Endpoint(requestType: String, responseType: String, errorTypes: List[String] = Nil, description: Option[String] = None)

abstract case class Contract private (endpoints: List[Endpoint], requests: ModelSet)

object Contract {
  def apply(endpoints: List[Endpoint], requestSet: RequestSet[_]): Contract = new Contract(endpoints, requestSet.modelSet) {}
  def empty: Contract                                                       = new Contract(Nil, ModelSet.models()) {}
}

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
