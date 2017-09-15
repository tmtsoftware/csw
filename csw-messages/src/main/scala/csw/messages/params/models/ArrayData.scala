package csw.messages.params.models

import java.util

import com.trueaccord.scalapb.{GeneratedMessageCompanion, TypeMapper}
import csw.param.pb.{ItemType, PbFormat}
import csw_params.parameter_types.Items
import spray.json.JsonFormat

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

case class ArrayData[T](data: mutable.WrappedArray[T]) {
  def values: Array[T]      = data.array
  def jValues: util.List[T] = data.asJava

  override def toString: String = data.mkString("(", ",", ")")
}

object ArrayData {
  import csw.messages.params.formats.JsonSupport._
  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[ArrayData[T]] =
    jsonFormat1((xs: mutable.WrappedArray[T]) ⇒ new ArrayData[T](xs))

  implicit def fromArray[T](xs: Array[T]): ArrayData[T] = new ArrayData(xs)

  def fromArray[T: ClassTag](xs: T*): ArrayData[T] = new ArrayData(xs.toArray[T])

  implicit def typeMapper[T: PbFormat: ClassTag]: TypeMapper[Items, ArrayData[T]] =
    new TypeMapper[Items, ArrayData[T]] {
      override def toCustom(base: Items): ArrayData[T] = PbFormat.arrayTypeMapper[T].toCustom(base)
      override def toBase(custom: ArrayData[T]): Items = PbFormat.arrayTypeMapper[T].toBase(custom.values)
    }

  implicit def typeMapper2[T: ClassTag, S <: ItemType[T, S]: GeneratedMessageCompanion]: TypeMapper[S, ArrayData[T]] =
    TypeMapper[S, ArrayData[T]](x ⇒ ArrayData(x.values.toArray[T]))(
      x ⇒ implicitly[GeneratedMessageCompanion[S]].defaultInstance.withValues(x.data)
    )
}

object JArrayData {
  def fromArray[T](array: Array[T]): ArrayData[T] = ArrayData.fromArray(array)
}
