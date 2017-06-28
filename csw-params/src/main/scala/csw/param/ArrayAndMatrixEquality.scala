package csw.param

import scala.annotation.tailrec
import scala.collection.immutable.Vector

/**
 * Utility functions for comparing arrays and vectors
 */
object ArrayAndMatrixEquality {

  /**
   * This function compares each element of two arrays for equality.
   * This function is used by the array items to test for equality.
   * @param one first array for comparison
   * @param two second array for comparison
   * @tparam T the Scala type of the arrays
   * @return true if the two arrays are equal, else false
   */
  def deepArrayValueEquals[T](one: Array[T], two: Array[T]): Boolean = {
    //one.deep == two.deep
    // Internet says sameElements is faster because of lack of matches
    one sameElements two
  }

  def arraySizeEqual(one: Array[_], two: Array[_]): Boolean = one.length == two.length

  def deepArrayEquals[T](one: Array[T], two: Array[T]): Boolean =
    arraySizeEqual(one, two) && deepArrayValueEquals(one, two)

  /**
   * This function takes Vectors of values from two items of the same type to compare their elements.
   * This function uses fold that works but looks at each vector regardless if one is discovered to be false
   *
   * @param one first Vector of type T for comparison
   * @param two second Vector of type T for comparison
   * @param f   a function that takes the T element and returns its Array of type U to be used for comparing
   * @tparam T the type of the containing object (i.e. LongArrayItem)
   * @tparam U the type of the array for comparing (i.e. Array[Long]
   * @return true if the arrays are equal or false if not
   */
  def vectorEquals1[T, U](one: Vector[T], two: Vector[T], f: T => Array[U]): Boolean = {
    val zipped: Vector[(T, T)] = one.zip(two)
    zipped.foldLeft(true) { (acc, v) =>
      deepArrayEquals(f(v._1), f(v._2))
    }
  }

  /**
   * This function takes Vectors of values from two items of the same type to compare their elements.
   * This recursive function will return as soon as one vector is not equal so is more efficient than
   * vectorEquals1.
   *
   * @param one first Vector of type T for comparison
   * @param two second Vector of type T for comparison
   * @param f   a function that takes the T element and returns its Array of type U to be used for comparing
   * @tparam T the type of the containing object (i.e. LongArrayItem)
   * @tparam U the type of the array for comparing (i.e. Array[Long]
   * @return true if the arrays are equal or false if not
   */
  def vectorEquals2[T, U](one: Vector[T], two: Vector[T], f: T => Array[U]): Boolean = {
    @tailrec
    def doCheck(v: Vector[(T, T)]): Boolean = {
      if (v.isEmpty) true
      else {
        if (!ArrayAndMatrixEquality.deepArrayEquals(f(v.head._1), f(v.head._2))) {
          false
        } else {
          doCheck(v.tail)
        }
      }
    }
    // Kick off check with intialized sequence
    doCheck(one.zip(two))
  }

  /**
   * This function is used by the Matrix item values to test equality. Right now it is comparing all values
   * which might not be appropriate for larger matrices
   *
   * @param one first matrix to compare
   * @param two second matrix to compare
   * @tparam T the Scala type of the values
   * @return True if the two matrices are equal
   */
  def deepMatrixValueEquals[T](one: Array[Array[T]], two: Array[Array[T]]): Boolean = {
    @tailrec
    def doCheck(v: Array[(Array[T], Array[T])]): Boolean = {
      if (v.isEmpty) true
      else {
        if (!ArrayAndMatrixEquality.deepArrayEquals(v.head._1, v.head._2)) {
          false
        } else {
          doCheck(v.tail)
        }
      }
    }
    // Kick off check with intialized sequence
    doCheck(one.zip(two))
  }

}
