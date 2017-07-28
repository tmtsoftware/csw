package csw.param

import csw.param.parameters.arrays.LongArray
import csw.param.parameters.matrices.LongMatrix
import org.scalatest.FunSpec

/**
 * TMT Source Code: 7/7/16.
 */
class ArrayAndMatrixEqualityTests extends FunSpec {
  import ArrayAndMatrixEquality._

  describe("arraySizeEqual") {
    val m1 = LongArray(Array(1, 2, 3))
    val m2 = LongArray(Array(1, 2, 3))
    val m3 = LongArray(Array(1, 2, 4, 8, 16))
    it("should notice if arrays are equal length") {
      assert(arraySizeEqual(m1.data, m2.data))
    }
    it("should notice if they aren't equal length") {
      assert(!arraySizeEqual(m1.data, m3.data))
    }
  }

  describe("deepArrayValueEquals") {
    val m1 = LongArray(Array(1, 2, 3))
    val m2 = LongArray(Array(1, 2, 3))
    val m3 = LongArray(Array(1, 2, 4))

    it("should see two arrays equal") {
      assert(deepArrayValueEquals(m1.data, m2.data))
    }
    it("should see to arrays that are not equal") {
      assert(!deepArrayValueEquals(m1.data, m3.data))
    }
  }

  describe("deepArrayValueEquals") {
    val m1 = LongArray(Array(1, 2, 3))
    val m2 = LongArray(Array(1, 2, 3))
    val m3 = LongArray(Array(1, 2, 4, 8, 16))
    val m4 = LongArray(Array(1, 2, 4, 9, 16))

    it("should succeed with equal lengths and values") {
      assert(deepArrayEquals(m1.data, m2.data))
    }
    it("should fail with different lengths") {
      assert(!deepArrayEquals(m1.data, m3.data))
    }
    it("should fail with same lengths, but different values") {
      assert(!deepArrayEquals(m2.data, m4.data))
    }
  }

  describe("vectorEquals2") {
    val m1 = LongArray(Array(1, 2, 3))
    val m2 = LongArray(Array(1, 2, 3))
    val m3 = LongArray(Array(1, 2, 4))

    val f = (l: LongArray) => l.data
    it("should notice equality for single member vectors of arrays") {
      // One member vectors, same values but different vectors, should be true
      assert(vectorEquals2(Vector(m1), Vector(m2), f))
    }
    it("should notice lack of equality for single member vectors of arrays") {
      // One member vectors, different in index 2, should be false
      assert(!vectorEquals2(Vector(m1), Vector(m3), f))
    }
    it("should see quality for vectors longer than 1") {
      // Multiple objects in each vector, same, should be true
      assert(vectorEquals1(Vector(m1, m3), Vector(m2, m3), f))
    }
    it("should see lack of equality for vectors longer than 1") {
      // Multiple objects in each vector, different in second object, should be false
      assert(!vectorEquals1(Vector(m1, m3), Vector(m1, m1), f))
    }
  }

  describe("deepMatrixValueEquals") {
    val m1 = LongMatrix(Array(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12)))
    val m2 = LongMatrix(Array(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12)))
    val m3 = LongMatrix(Array(Array(1, 2, 3), Array(2, 3, 0), Array(4, 6, 12)))

    it("should return true for the same matrix") {
      assert(deepMatrixValueEquals(m1.data, m1.data))
    }
    it("should return true for two different matrices with the same values") {
      assert(deepMatrixValueEquals(m1.data, m2.data))
    }
    it("should return false for two matrices with different values") {
      assert(!deepMatrixValueEquals(m2.data, m3.data))
    }
  }

}
