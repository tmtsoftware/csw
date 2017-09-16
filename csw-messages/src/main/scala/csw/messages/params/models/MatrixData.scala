package csw.messages.params.models

import java.util

import com.trueaccord.scalapb.TypeMapper
import csw.param.pb.{ItemType, ItemTypeCompanion}
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

  implicit def typeMapper2[T: ClassTag, S <: ItemType[ArrayData[T]]: ItemTypeCompanion]: TypeMapper[S, MatrixData[T]] =
    TypeMapper[S, MatrixData[T]](x ⇒ MatrixData.fromArrays(x.values.toArray.map(a ⇒ a.data.array)))(
      x ⇒ ItemTypeCompanion[S].defaultInstance.withValues2(x.data.map(ArrayData.apply))
    )
}

object JMatrixData {
  def fromArrays[T](klass: Class[T], xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))(ClassTag(klass))
}
