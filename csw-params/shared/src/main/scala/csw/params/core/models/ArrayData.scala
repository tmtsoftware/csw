package csw.params.core.models

import com.github.ghik.silencer.silent

import scala.annotation.varargs
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._

/**
 * A top level key for a parameter set representing an array like collection.
 *
 * @param data input array
 */
case class ArrayData[T](data: mutable.ArraySeq[T]) {

  /**
   * A Java helper that returns an Array of values this parameter holds
   */
  def jValues: java.util.List[T] = data.asJava

  /**
   * An Array of values this parameter holds
   */
  def values: Array[T] = data.array.asInstanceOf[Array[T]]

  /**
   * A comma separated string representation of all values this ArrayData holds
   */
  override def toString: String = data.mkString("(", ",", ")")
}

object ArrayData {

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
   * @param rest one or more values
   * @tparam T the type of values
   * @return an instance of ArrayData
   */
  @varargs
  def fromArrays[T](first: T, rest: T*): ArrayData[T] = {
    @silent
    implicit val ct: ClassTag[T] = ClassTag[T](first.getClass)
    ArrayData.fromArray((first +: rest).toArray)
  }
}
