package csw.messages.params.models

import java.util

import com.trueaccord.scalapb.TypeMapper
import csw.messages.params.pb.{ItemType, ItemTypeCompanion}
import play.api.libs.json.{Format, Json}

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

  implicit def format[T: Format: ClassTag]: Format[MatrixData[T]] = Json.format[MatrixData[T]]

  implicit def fromArrays[T: ClassTag](xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))

  def fromArrays[T: ClassTag](xs: Array[T]*): MatrixData[T] =
    new MatrixData[T](xs.toArray.map(x ⇒ x: mutable.WrappedArray[T]))

  implicit def typeMapper2[T: ClassTag, S <: ItemType[ArrayData[T]]: ItemTypeCompanion]: TypeMapper[S, MatrixData[T]] =
    TypeMapper[S, MatrixData[T]](x ⇒ MatrixData.fromArrays(x.values.toArray.map(a ⇒ a.data.array)))(
      x ⇒ ItemTypeCompanion.make(x.data.map(ArrayData.apply))
    )

  implicit def conversion[A, B](implicit conversion: A ⇒ B): MatrixData[A] ⇒ MatrixData[B] =
    _.asInstanceOf[MatrixData[B]]
}

object JMatrixData {
  def fromArrays[T](klass: Class[T], xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))(ClassTag(klass))
}
