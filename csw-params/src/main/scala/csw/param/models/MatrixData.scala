package csw.param.models

import csw.param.formats.WrappedArrayProtocol
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

case class MatrixData[T](data: mutable.WrappedArray[mutable.WrappedArray[T]]) {
  def apply(row: Int, col: Int): T = data(row)(col)
}

object MatrixData extends WrappedArrayProtocol with DefaultJsonProtocol {
  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[MatrixData[T]] =
    jsonFormat1((xs: mutable.WrappedArray[mutable.WrappedArray[T]]) => new MatrixData[T](xs))

  implicit def fromArrays[T](xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))

  def fromArrays[T: ClassTag](xs: Array[T]*): MatrixData[T] =
    new MatrixData[T](xs.toArray.map(x ⇒ x: mutable.WrappedArray[T]))
}
