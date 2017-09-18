package csw.param

import csw.param.generics.{KeyType, Parameter}
import csw.param.models.{ArrayData, MatrixData}
import csw.units.Units
import csw_params.parameter.PbParameter
import csw_params.parameter_types._
import org.scalatest.FunSuite
import spray.json.JsonFormat

class DD2 extends FunSuite {

  test("1") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.centimeter)
      .withIntItems(IntItems().addValues(1, 2))

    val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)
    val value: PbParameter      = PbParameter.parseFrom(parameter1.toByteString.toByteArray)
    println(value)
    println(parameter1 === value)
  }

  test("2") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withIntItems(IntItems().withValues(Seq(1, 2, 3, 4)))

    val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)
    val value                   = PbParameter.parseFrom(parameter1.toByteString.toByteArray)

    println(value)
    println(parameter1 === value)
  }

  test("3") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withIntArrayItems(IntArrayItems().addValues(Array(1000, 2000, 3000), Array(-1, -2, -3)))

    println(parameter.getIntArrayItems.toString)
  }

  test("4") {
    val key   = KeyType.IntKey.make("encoder")
    val param = key.set(1, 2, 3, 4)

    val value = PbParameter()
      .withName("encoder")
      .withUnits(Units.angstrom)
      .withIntItems(IntItems().addValues(1, 2, 3, 4))

    println("---------------")
    println(value.items)

    val out = PbParameter.parseFrom(value.toByteArray)

    println(value)
    println(out)
    println(out === param)

    val key2   = KeyType.StringKey.make("encoder")
    val param2 = key2.set("abc", "xyc")

    val value2 = PbParameter()
      .withName("encoder")
      .withStringItems(StringItems().addValues("abc", "xyz"))
    val out2 = PbParameter.parseFrom(value2.toByteArray)

    println(value2)
    println(out2)
    println(out2 === param2)
  }

  test("6") {
    import spray.json.DefaultJsonProtocol._
    val key    = KeyType.IntKey.make("encoder")
    val param  = key.set(1, 2, 3, 4)
    val mapper = Parameter.typeMapper[Int]
    val value  = mapper.toCustom(mapper.toBase(param))
    println(value)
  }

  test("7") {
    println(PbParameter.scalaDescriptor.fields)
    val dd2: IntMatrixItems = IntMatrixItems().set(
      Seq(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)), MatrixData.fromArrays(Array(11, 12, 13), Array(16, 17)))
    )
    println(dd2.values.head.values.head.head)
  }

  test("8") {
    import spray.json.DefaultJsonProtocol._
    val value = Parameter.typeMapper[ArrayData[Int]]
    val key   = KeyType.IntArrayKey.make("blah")
    val param = key.set(ArrayData.fromArray(1, 2, 3))
    println(value.toCustom(value.toBase(param)))
  }

  test("9") {
    import spray.json.DefaultJsonProtocol._
    val value  = Parameter.typeMapper[MatrixData[Int]]
    val key    = KeyType.IntMatrixKey.make("blah")
    val param  = key.set(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)))
    val result = value.toCustom(value.toBase(param))
    println(result)
    println(result.keyType)
  }

}
