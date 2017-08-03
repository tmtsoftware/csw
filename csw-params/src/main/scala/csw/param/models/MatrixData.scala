package csw.param.models

import java.util

import spray.json.JsonFormat

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

case class MatrixData[T](data: mutable.WrappedArray[mutable.WrappedArray[T]]) {
  def apply(row: Int, col: Int): T = data(row)(col)

  def values(implicit classTag: ClassTag[T]): Array[Array[T]] = data.array.map(_.array)

  def jValuesArray(klass: Class[T]): Array[Array[T]] = values(ClassTag(klass))

  def jValuesList: util.List[util.List[T]] = data.map(_.asJava).asJava
}

object MatrixData {
  import csw.param.JsonSupport._

  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[MatrixData[T]] =
    jsonFormat1((xs: mutable.WrappedArray[mutable.WrappedArray[T]]) => new MatrixData[T](xs))

  implicit def fromArrays[T](xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))

  def fromArrays[T: ClassTag](xs: Array[T]*): MatrixData[T] =
    new MatrixData[T](xs.toArray.map(x ⇒ x: mutable.WrappedArray[T]))
}

object JMatrixData {
  def fromArrays[T](xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))
}
