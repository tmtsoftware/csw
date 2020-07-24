package csw.params.core.models

import java.util

import scala.annotation.{nowarn, varargs}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * A top level key for a parameter set representing an matrix like collection.
 *
 * @param data input array of array
 */
case class MatrixData[T] private (data: mutable.ArraySeq[mutable.ArraySeq[T]])(val values: Array[Array[T]]) {

  /**
   * Returns a value stored at position represented by [row][col]
   *
   * @return a value represented by T
   */
  def apply(row: Int, col: Int): T = data(row)(col)

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

  /**
   * Create a MatrixData from one or more arrays of Array[T]
   *
   * @param values one or more arrays
   * @tparam T the type of values
   * @return an instance of MatrixData
   */
  implicit def fromArrays[T](values: Array[Array[T]]): MatrixData[T] =
    new MatrixData[T](values.map(x => x: mutable.ArraySeq[T]))(values) {}

  /**
   * Create a MatrixData from Array[T]
   *
   * @param rest one or more arrays
   * @tparam T the type of values
   * @return an instance of MatrixData
   */
  @varargs
  def fromArrays[T](first: Array[T], rest: Array[T]*): MatrixData[T] = {
    // getComponentType gives the Class type of T from Array[T]
    @nowarn
    implicit val ct: ClassTag[T] = ClassTag[T](first.getClass.getComponentType)
    MatrixData.fromArrays((first +: rest).toArray)
  }
}
