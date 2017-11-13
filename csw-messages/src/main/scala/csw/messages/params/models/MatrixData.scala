package csw.messages.params.models

import java.util

import com.trueaccord.scalapb.TypeMapper
import csw.messages.params.pb.{ItemType, ItemTypeCompanion}
import play.api.libs.json.{Format, Json}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * A top level key for a parameter set representing an matrix like collection.
 *
 * @param data input array of array
 */
case class MatrixData[T](data: mutable.WrappedArray[mutable.WrappedArray[T]])(implicit cTag: ClassTag[T]) {

  /**
   * returns a value stored at position represented by [row][col]
   *
   * @return a value represented by T
   */
  def apply(row: Int, col: Int): T = data(row)(col)

  //scala
  def values: Array[Array[T]] = data.array.map(_.array)
  //java
  def jValues: util.List[util.List[T]] = data.map(_.asJava).asJava

  override def toString: String = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")
}

object MatrixData {

  //play-json formatter
  implicit def format[T: Format: ClassTag]: Format[MatrixData[T]] = Json.format[MatrixData[T]]

  /**
   * constructs a MatrixData from a given Array[Array[T]]
   */
  implicit def fromArrays[T: ClassTag](xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))

  /**
   * constructs a MatrixData from a given Array[T]
   */
  def fromArrays[T: ClassTag](xs: Array[T]*): MatrixData[T] =
    new MatrixData[T](xs.toArray.map(x ⇒ x: mutable.WrappedArray[T]))

  //Protobuf converter
  implicit def typeMapper[T: ClassTag, S <: ItemType[ArrayData[T]]: ItemTypeCompanion]: TypeMapper[S, MatrixData[T]] =
    TypeMapper[S, MatrixData[T]](x ⇒ MatrixData.fromArrays(x.values.toArray.map(a ⇒ a.data.array)))(
      x ⇒ ItemTypeCompanion.make(x.data.map(ArrayData.apply))
    )

  implicit def conversion[A, B](implicit conversion: A ⇒ B): MatrixData[A] ⇒ MatrixData[B] =
    _.asInstanceOf[MatrixData[B]]
}

/**
 * Helper functions for Java
 */
object JMatrixData {
  def fromArrays[T](klass: Class[T], xs: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))(ClassTag(klass))
}
