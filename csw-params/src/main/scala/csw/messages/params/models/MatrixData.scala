package csw.messages.params.models

import java.util

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
   * Returns a value stored at position represented by [row][col]
   *
   * @return a value represented by T
   */
  def apply(row: Int, col: Int): T = data(row)(col)

  /**
   * An Array of values this parameter holds
   */
  def values: Array[Array[T]] = data.array.map(_.array)

  /**
   * A Java helper that returns an Array of values this parameter holds
   */
  def jValues: util.List[util.List[T]] = data.map(_.asJava).asJava

  /**
   * A comma separated string representation of all values this MatrixData holds
   */
  override def toString: String = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")
}

object MatrixData {

  //play-json formatter
  private[messages] implicit def format[T: Format: ClassTag]: Format[MatrixData[T]] = Json.format[MatrixData[T]]

  /**
   * Create a MatrixData from one or more arrays of Array[T]
   *
   * @param values one or more arrays
   * @tparam T the type of values
   * @return an instance of MatrixData
   */
  implicit def fromArrays[T: ClassTag](values: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](values.map(x ⇒ x: mutable.WrappedArray[T]))

  /**
   * Create a MatrixData from Array[T]
   *
   * @param values one or more arrays
   * @tparam T the type of values
   * @return an instance of MatrixData
   */
  def fromArrays[T: ClassTag](values: Array[T]*): MatrixData[T] =
    new MatrixData[T](values.toArray.map(x ⇒ x: mutable.WrappedArray[T]))

  /**
   * A Java helper to create an MatrixData from one or more arrays
   *
   * @param values an Array of one or more array of values
   * @tparam T the type of values
   * @return an instance of MatrixData
   */
  def fromJavaArrays[T](klass: Class[T], values: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](values.map(x ⇒ x: mutable.WrappedArray[T]))(ClassTag(klass))

  /**
   * Convert a Matrix of data from one type to other
   *
   * @param conversion a function of type A => B
   * @tparam A the source type of data
   * @tparam B the destination type of data
   * @return a function of type MatrixData[A] ⇒ MatrixData[B]
   */
  implicit def conversion[A, B](implicit conversion: A ⇒ B): MatrixData[A] ⇒ MatrixData[B] =
    _.asInstanceOf[MatrixData[B]]
}
