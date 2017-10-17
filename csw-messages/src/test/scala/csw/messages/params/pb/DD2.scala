package csw.messages.params.pb

import java.time.Instant

import com.trueaccord.scalapb.json.JsonFormat
import csw.messages.params.generics.{JKeyTypes, KeyType, Parameter}
import csw.messages.params.models._
import csw_messages_params.ParameterTypes
import csw_messages_params.parameter.PbParameter
import csw_messages_params.parameter_types._
import csw_messages_params.radec.PbRaDec
import org.scalatest.FunSuite

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
    val value = Parameter.typeMapper2
    val key   = KeyType.IntArrayKey.make("blah")
    val param = key.set(ArrayData.fromArray(1, 2, 3))
    println(value.toCustom(value.toBase(param)))
  }

  test("9") {
    val value  = Parameter.typeMapper2
    val key    = KeyType.IntMatrixKey.make("blah")
    val param  = key.set(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)))
    val result = value.toCustom(value.toBase(param))
    println(result)
    println(result.keyType)
  }

  test("10") {
    val p: ParameterTypes.BooleanItems =
      ParameterTypes.BooleanItems.newBuilder().addValues(true).addValues(false).build()

    val items = BooleanItems.parseFrom(p.toByteString.toByteArray)

    println(p)
    println(items)
  }

  test("11") {
    val key    = JKeyTypes.IntKey.make("encoder")
    val param  = key.set(1, 2, 3, 4)
    val mapper = Parameter.typeMapper2
    val value  = mapper.toCustom(mapper.toBase(param))
    println(value)
  }

  test("12") {
    val parameter = PbParameter()
      .withName("abcd")
      .withUnits(Units.second)
      .withKeyType(KeyType.IntKey)
      .withCharItems(CharItems().set(Seq('a', 'b')))
      .withIntItems(IntItems().addValues(1))
    println(parameter)
  }

  test("13") {
    val parameter = PbParameter()
      .withName("abcd")
      .withUnits(Units.second)
      .withKeyType(KeyType.TimestampKey)
      .withInstantItems(InstantItems(Seq(Instant.now)))
    println(parameter)
  }

  test("14") {
    val parameter = PbParameter()
      .withName("abcd")
      .withUnits(Units.second)
      .withKeyType(KeyType.TimestampKey)
      .withByteItems(ByteItems(Seq(1, 2, 3, 4)))
    println(parameter)
  }

  test("15") {
    val choices = ChoiceItems().set(Seq("a345", "b234", "c567", "d890"))
    val parameter = PbParameter()
      .withName("abcd")
      .withUnits(Units.second)
      .withKeyType(KeyType.TimestampKey)
      .withChoiceItems(choices)
    println(JsonFormat.toJsonString(parameter))
    println(JsonFormat.toJson(parameter))
  }

  test("16") {
    PbRaDec()
    PbRaDec.defaultInstance

    PbRaDec().withRa(10).withDec(32)
    PbRaDec().withRa(10).withDec(321)

    val raDec = RaDec(1, 2)
    val param = KeyType.RaDecKey.make("Asd").set(raDec)

    val pbParameter = Parameter.typeMapper2.toBase(param)
    pbParameter.toByteArray
    println(pbParameter)
  }

  test("17") {

    val p1 = PbParameter()
      .withName("a")
//      .withKeyType(KeyType.BooleanKey)
      .withIntItems(IntItems().addValues(1, 2))

    println(p1)
  }

  test("18") {
    val pbParam = PbParameter().withIntItems(IntItems().addValues(1, 2, 3, 4, 5))
    println(pbParam)
  }
}
