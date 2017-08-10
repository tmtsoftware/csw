package csw.param

import csw.param.generics.KeyType
import csw.param.generics.KeyType.{
  ByteMatrixKey,
  DoubleMatrixKey,
  FloatMatrixKey,
  IntMatrixKey,
  LongMatrixKey,
  ShortMatrixKey
}
import csw.param.models.{ArrayData, MatrixData}
import org.scalatest.FunSpec

class EqualityTest extends FunSpec {

  describe("Array-based long array equality") {
    val k1                  = KeyType.LongArrayKey.make("myLongArray")
    val m1: ArrayData[Long] = ArrayData(Array(1, 2, 3))
    val m2: ArrayData[Long] = ArrayData(Array(1, 2, 3))
    val m3: ArrayData[Long] = ArrayData(Array(1, 2, 4))
    val i1                  = k1.set(m1)
    val i2                  = k1.set(m2)
    val i3                  = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }
    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }
    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }

  }

  describe("Array-based long matrix equality") {
    val k1                   = LongMatrixKey.make("myMatrix")
    val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12))
    val m2: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12))
    val m3: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(0, 6, 12)) // Note one value different
    val m4: MatrixData[Long] = MatrixData.fromArrays(Array(1, 0, 0), Array(0, 1, 0), Array(0, 0, 1))
    val i1                   = k1.set(m1)
    val i2                   = k1.set(m2)
    val i3                   = k1.set(m3)
    val i4                   = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based byte array equality") {
    val k1 = KeyType.ByteArrayKey.make("myByteArray")
    val m1 = ArrayData(Array[Byte](1, 2, 3))
    val m2 = ArrayData(Array[Byte](1, 2, 3))
    val m3 = ArrayData(Array[Byte](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based byte matrix equality") {
    val k1 = ByteMatrixKey.make("myMatrix")
    val m1 = MatrixData.fromArrays(Array[Byte](1, 2, 3), Array[Byte](2, 3, 6), Array[Byte](4, 6, 12))
    val m2 = MatrixData.fromArrays(Array[Byte](1, 2, 3), Array[Byte](2, 3, 6), Array[Byte](4, 6, 12))
    val m3 = MatrixData.fromArrays(Array[Byte](1, 2, 3), Array[Byte](2, 3, 6), Array[Byte](0, 6, 12)) // Note one value different
    val m4 = MatrixData.fromArrays(Array[Byte](1, 0, 0), Array[Byte](0, 1, 0), Array[Byte](0, 0, 1))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based double array equality") {
    val k1 = KeyType.DoubleArrayKey.make("myArray")
    val m1 = ArrayData(Array[Double](1, 2, 3))
    val m2 = ArrayData(Array[Double](1, 2, 3))
    val m3 = ArrayData(Array[Double](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based double matrix equality") {
    val k1 = DoubleMatrixKey.make("myMatrix")
    val m1 = MatrixData.fromArrays(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](4, 6, 12))
    val m2 = MatrixData.fromArrays(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](4, 6, 12))
    val m3 = MatrixData.fromArrays(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](0, 6, 12)) // Note one value different
    val m4 = MatrixData.fromArrays(Array[Double](1, 0, 0), Array[Double](0, 1, 0), Array[Double](0, 0, 1))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based float array equality") {
    val k1 = KeyType.FloatArrayKey.make("myArray")
    val m1 = ArrayData(Array[Float](1, 2, 3))
    val m2 = ArrayData(Array[Float](1, 2, 3))
    val m3 = ArrayData(Array[Float](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based float matrix equality") {
    val k1 = FloatMatrixKey.make("myMatrix")
    val m1 = MatrixData.fromArrays(Array[Float](1, 2, 3), Array[Float](2, 3, 6), Array[Float](4, 6, 12))
    val m2 = MatrixData.fromArrays(Array[Float](1, 2, 3), Array[Float](2, 3, 6), Array[Float](4, 6, 12))
    val m3 = MatrixData.fromArrays(Array[Float](1, 2, 3), Array[Float](2, 3, 6), Array[Float](0, 6, 12)) // Note one value different
    val m4 = MatrixData.fromArrays(Array[Float](1, 0, 0), Array[Float](0, 1, 0), Array[Float](0, 0, 1))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based int array equality") {
    val k1 = KeyType.IntArrayKey.make("myArray")
    val m1 = Array[Int](1, 2, 3)
    val m2 = Array[Int](1, 2, 3)
    val m3 = Array[Int](1, 2, 4)
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based int matrix equality") {
    val k1                  = IntMatrixKey.make("myMatrix")
    val m1: MatrixData[Int] = MatrixData.fromArrays(Array[Int](1, 2, 3), Array[Int](2, 3, 6), Array[Int](4, 6, 12))
    val m2: MatrixData[Int] = MatrixData.fromArrays(Array[Int](1, 2, 3), Array[Int](2, 3, 6), Array[Int](4, 6, 12))
    val m3: MatrixData[Int] = MatrixData.fromArrays(Array[Int](1, 2, 3), Array[Int](2, 3, 6), Array[Int](0, 6, 12)) // Note one value different
    val m4: MatrixData[Int] = MatrixData.fromArrays(Array[Int](1, 0, 0), Array[Int](0, 1, 0), Array[Int](0, 0, 1))
    val i1                  = k1.set(m1)
    val i2                  = k1.set(m2)
    val i3                  = k1.set(m3)
    val i4                  = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based short array equality") {
    val k1 = KeyType.ShortArrayKey.make("myArray")
    val m1 = ArrayData(Array[Short](1, 2, 3))
    val m2 = ArrayData(Array[Short](1, 2, 3))
    val m3 = ArrayData(Array[Short](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based short matrix equality") {
    val k1 = ShortMatrixKey.make("myMatrix")
    val m1 = MatrixData.fromArrays(Array[Short](1, 2, 3), Array[Short](2, 3, 6), Array[Short](4, 6, 12))
    val m2 = MatrixData.fromArrays(Array[Short](1, 2, 3), Array[Short](2, 3, 6), Array[Short](4, 6, 12))
    val m3 = MatrixData.fromArrays(Array[Short](1, 2, 3), Array[Short](2, 3, 6), Array[Short](0, 6, 12)) // Note one value different
    val m4 = MatrixData.fromArrays(Array[Short](1, 0, 0), Array[Short](0, 1, 0), Array[Short](0, 0, 1))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }
}
