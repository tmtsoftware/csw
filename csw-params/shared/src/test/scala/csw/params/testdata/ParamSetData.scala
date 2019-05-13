package csw.params.testdata

import java.time.Instant

import csw.params.core.generics.KeyType._
import csw.params.core.generics.Parameter
import csw.params.core.models.{ArrayData, Choices, MatrixData, RaDec, Struct}
import csw.time.core.models.{TAITime, UTCTime}

object ParamSetData {
  // Simple Key's
  private val p1 = BooleanKey.make("BooleanKey").set(true, false)
  private val p2 = ByteKey.make("ByteKey").set(Array[Byte](10, 20))
  private val p3 = CharKey.make("CharKey").set('A', 'B')
  private val p4 = ShortKey.make("ShortKey").set(Array[Short](30, 40))
  private val p5 = LongKey.make("LongKey").set(Array[Long](50, 60))
  private val p6 = IntKey.make("IntKey").set(70, 80)
  private val p7 = FloatKey.make("FloatKey").set(Array[Float](90, 100))
  private val p8 = DoubleKey.make("DoubleKey").set(Array[Double](110, 120))
  private val p9 =
    UTCTimeKey
      .make("UTCTimeKey")
      .set(UTCTime(Instant.ofEpochMilli(0)), UTCTime(Instant.parse("2017-09-04T19:00:00.123456789Z")))

  private val p10 =
    TAITimeKey
      .make("TAITimeKey")
      .set(TAITime(Instant.ofEpochMilli(0)), TAITime(Instant.parse("2017-09-04T19:00:00.123456789Z")))

  // ArrayData Key's
  private val p11 = ByteArrayKey.make("ByteArrayKey").set(ArrayData.fromArray(Array[Byte](1, 2)))
  private val p12 = ShortArrayKey.make("ShortArrayKey").set(ArrayData.fromArray(Array[Short](3, 4)))
  private val p13 = LongArrayKey.make("LongArrayKey").set(ArrayData.fromArray(Array[Long](5, 6)))
  private val p14 = IntArrayKey.make("IntArrayKey").set(ArrayData.fromArray(Array(7, 8)))
  private val p15 = FloatArrayKey.make("FloatArrayKey").set(ArrayData.fromArray(Array[Float](9, 10)))
  private val p16 = DoubleArrayKey.make("DoubleArrayKey").set(ArrayData.fromArray(Array[Double](11, 12)))
  // MatrixData Key's
  private val p17 = ByteMatrixKey.make("ByteMatrix").set(MatrixData.fromArrays(Array[Byte](1, 2), Array[Byte](3, 4)))
  private val p18 = ShortMatrixKey.make("ShortMatrix").set(MatrixData.fromArrays(Array[Short](4, 5), Array[Short](6, 7)))
  private val p19 = LongMatrixKey.make("LongMatrix").set(MatrixData.fromArrays(Array[Long](8, 9), Array[Long](10, 11)))
  private val p20 = IntMatrixKey.make("IntMatrix").set(MatrixData.fromArrays(Array(12, 13), Array(14, 15)))
  private val p21 = FloatMatrixKey.make("FloatMatrix").set(MatrixData.fromArrays(Array[Float](16, 17), Array[Float](18, 19)))
  private val p22 = DoubleMatrixKey.make("DoubleMatrix").set(MatrixData.fromArrays(Array[Double](20, 21), Array[Double](22, 23)))
  // RaDec Key
  private val p23 = RaDecKey.make("RaDecKey").set(RaDec(7.3, 12.1))
  // Choice Key
  private val p24 = ChoiceKey.make("ChoiceKey", Choices.from("First", "Second")).set("First", "Second")
  // Struct Key
  private val p25 = StructKey.make("StructKey").set(Struct(Set(p1, p2)))
  private val p26 = StringKey.make("StringKey").set("Str1", "Str2")

  val paramSet: Set[Parameter[_]] =
    Set(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26)

}
