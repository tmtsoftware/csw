package csw.messages.params.models

import scalapb.TypeMapper
import csw.messages.params.generics.{Parameter, ParameterSetType}
import csw_protobuf.parameter.PbStruct
import play.api.libs.json.{Json, OFormat}

import scala.annotation.varargs
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

/**
 * A configuration for setting telescope and instrument parameters
 *
 * @param paramSet set of Parameters
 */
case class Struct private (paramSet: Set[Parameter[_]]) extends ParameterSetType[Struct] {

  /**
   * This is here for Java to construct with String
   */
  def this() = this(Set.empty[Parameter[_]])

  override protected def create(data: Set[Parameter[_]]) = new Struct(data)

  override def toString = paramSet.mkString(", ")
}

object Struct {
  //used by play-json
  implicit val format: OFormat[Struct] = Json.format[Struct]

  def apply(paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Struct = new Struct().madd(paramSet)

  //Protobuf converter
  implicit val typeMapper: TypeMapper[PbStruct, Struct] = TypeMapper[PbStruct, Struct] { s =>
    Struct(s.paramSet.map(Parameter.typeMapper2.toCustom).toSet)
  } { s =>
    PbStruct().withParamSet(s.paramSet.map(Parameter.typeMapper2.toBase).toSeq)
  }
}

/**
 * Java helpers.
 */
object JStruct {

  @varargs
  def create(data: Parameter[_]*): Struct = Struct(data.toSet)

  def create(data: java.util.Set[Parameter[_]]): Struct = Struct(data.asScala.toSet)
}
