package csw.messages.params.models

import java.util

import play.api.libs.json.{Format, Json}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * A top level key for a parameter set representing an array like collection.
 *
 * @param data input array
 */
case class ArrayData[T](data: mutable.WrappedArray[T]) {

  /**
   * An Array of values this parameter holds
   */
  def values: Array[T] = data.array

  /**
   * A Java helper that returns an Array of values this parameter holds
   */
  def jValues: util.List[T] = data.asJava

  /**
   * A comma separated string representation of all values this ArrayData holds
   */
  override def toString: String = data.mkString("(", ",", ")")
}

object ArrayData {
  private[messages] implicit def format[T: Format: ClassTag]: Format[ArrayData[T]] = Json.format[ArrayData[T]]

  /**
   * Create an ArrayData from one or more values
   *
   * @param values an Array of one or more values
   * @tparam T the type of values
   * @return an instance of ArrayData
   */
  implicit def fromArray[T](values: Array[T]): ArrayData[T] = new ArrayData(values)

  /**
   * Create an ArrayData from one or more values
   *
   * @param values one or more values
   * @tparam T the type of values
   * @return an instance of ArrayData
   */
  def fromArray[T: ClassTag](values: T*): ArrayData[T] = new ArrayData(values.toArray[T])

  /**
   * A Java helper to create an ArrayData from one or more values
   *
   * @param values an Array of one or more values
   * @tparam T the type of values
   * @return an instance of ArrayData
   */
  def fromJavaArray[T](values: Array[T]): ArrayData[T] = ArrayData.fromArray(values)

  /**
   * Convert an Array of data from one type to other
   *
   * @param conversion a function of type A => B
   * @tparam A the source type of data
   * @tparam B the destination type of data
   * @return a function of type ArrayData[A] ⇒ ArrayData[B]
   */
  implicit def conversion[A, B](implicit conversion: A ⇒ B): ArrayData[A] ⇒ ArrayData[B] = _.asInstanceOf[ArrayData[B]]
}
