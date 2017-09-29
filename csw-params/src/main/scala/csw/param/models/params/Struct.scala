package csw.param.models.params

import csw.param.generics.{Parameter, ParameterSetType}
import spray.json.JsonFormat

import scala.annotation.varargs
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

case class Struct private (paramSet: Set[Parameter[_]]) extends ParameterSetType[Struct] {

  /**
   * This is here for Java to construct with String
   */
  def this() = this(Set.empty[Parameter[_]])

  override protected def create(data: Set[Parameter[_]]) = new Struct(data)

  override def toString = paramSet.mkString(", ")
}

object Struct {

  import spray.json.DefaultJsonProtocol._
  implicit val format: JsonFormat[Struct] = jsonFormat1(Struct.apply)

  def apply(paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Struct = new Struct().madd(paramSet)
}

object JStruct {

  @varargs
  def create(data: Parameter[_]*): Struct = Struct(data.toSet)

  def create(data: java.util.Set[Parameter[_]]): Struct = Struct(data.asScala.toSet)
}
