package csw.messages.params.models

import java.util

import com.google.protobuf.ByteString
import com.trueaccord.scalapb.TypeMapper
import csw.param.pb.PbFormat
import csw_params.parameter_types.Items
import spray.json.JsonFormat

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

case class MatrixData[T](data: mutable.WrappedArray[mutable.WrappedArray[T]])(implicit @transient cTag: ClassTag[T]) {
  def apply(row: Int, col: Int): T = data(row)(col)

  def values: Array[Array[T]]          = data.array.map(_.array)
  def jValues: util.List[util.List[T]] = data.map(_.asJava).asJava

  override def toString: String = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")
}

object MatrixData {
  import csw.messages.params.formats.JsonSupport._

  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[MatrixData[T]] =
    jsonFormat1((xs: mutable.WrappedArray[mutable.WrappedArray[T]]) => new MatrixData[T](xs))

  implicit def fromArrays[T: ClassTag](xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))

  def fromArrays[T: ClassTag](xs: Array[T]*): MatrixData[T] =
    new MatrixData[T](xs.toArray.map(x ⇒ x: mutable.WrappedArray[T]))

  implicit def typeMapper[T: PbFormat: ClassTag]: TypeMapper[Items, MatrixData[T]] =
    new TypeMapper[Items, MatrixData[T]] {
      override def toCustom(base: Items): MatrixData[T] =
        MatrixData.fromArrays(PbFormat.arrayTypeMapper[Array[T]].toCustom(base))
      override def toBase(custom: MatrixData[T]): Items =
        PbFormat.arrayTypeMapper[Array[T]].toBase(custom.values)
    }

}

object JMatrixData {
  def fromArrays[T](klass: Class[T], xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))(ClassTag(klass))
}
