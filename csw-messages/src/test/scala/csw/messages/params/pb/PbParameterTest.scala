package csw.messages.params.pb

import java.time.Instant

import csw.messages.params.generics.KeyType.IntMatrixKey
import csw.messages.params.generics.{JKeyTypes, KeyType, Parameter}
import csw.messages.params.models._
import csw_messages_params.ParameterTypes
import csw_messages_params.parameter.PbParameter
import csw_messages_params.parameter_types._
import csw_messages_params.radec.PbRaDec
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

// DEOPSCSW-297: Merge protobuf branch in master
class PbParameterTest extends FunSuite with Matchers {

  test("should able to parse PbParameter with multiple values") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.centimeter)
      .withIntItems(IntItems().addValues(1, 2))

    val pbParam: PbParameter       = PbParameter.parseFrom(parameter.toByteArray)
    val parsedPbParam: PbParameter = PbParameter.parseFrom(pbParam.toByteString.toByteArray)

    pbParam shouldBe parsedPbParam
  }

  test("should able to parse PbParameter with sequence of values") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withIntItems(IntItems().withValues(Seq(1, 2, 3, 4)))

    val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)
    val parsedParameter         = PbParameter.parseFrom(parameter1.toByteString.toByteArray)

    parameter shouldEqual parsedParameter
  }

  test("should able to create Boolean Items") {
    val booleanItems: ParameterTypes.BooleanItems =
      ParameterTypes.BooleanItems.newBuilder().addValues(true).addValues(false).build()

    val parsedBooleanItems = BooleanItems.parseFrom(booleanItems.toByteString.toByteArray)

    booleanItems.getValuesList.asScala.toSeq shouldBe parsedBooleanItems.values
  }

  test("should able to create PbParameter with Timestamp items") {
    val now = Instant.now
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.TimestampKey)
      .withInstantItems(InstantItems(Seq(now)))

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.TimestampKey
    parameter.getInstantItems.values shouldBe List(now)
  }

  test("should able to create PbParameter with Byte items") {
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.TimestampKey)
      .withByteItems(ByteItems(Seq(1, 2, 3, 4)))

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.TimestampKey
    parameter.getByteItems.values shouldBe Seq(1, 2, 3, 4)
  }

  test("should able to create PbParameter with Choice items") {
    val choices = ChoiceItems().set(Seq("a345", "b234", "c567", "d890"))
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.TimestampKey)
      .withChoiceItems(choices)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.TimestampKey
  }

  test("should able to create PbParameter with int items only when KeyType is Int") {
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.IntKey)
      .withCharItems(CharItems().set(Seq('a', 'b')))
      .withIntItems(IntItems().addValues(1))

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.IntKey
    parameter.getIntItems.values shouldBe List(1)
    parameter.getCharItems.values shouldBe List.empty
  }

  test("should able to create PbParameter and compare with Parameter for Int and String Key") {
    val key   = KeyType.IntKey.make("encoder")
    val param = key.set(1, 2, 3, 4)

    val pbParam = PbParameter()
      .withName("encoder")
      .withUnits(Units.angstrom)
      .withIntItems(IntItems().addValues(1, 2, 3, 4))

    val items: IntItems = pbParam.items.value.asInstanceOf[IntItems]
    items.values shouldBe Seq(1, 2, 3, 4)

    val parsedPbParam = PbParameter.parseFrom(pbParam.toByteArray)

    pbParam shouldBe parsedPbParam
    parsedPbParam should not be param

    val key2   = KeyType.StringKey.make("encoder")
    val param2 = key2.set("abc", "xyc")

    val pbParam2 = PbParameter()
      .withName("encoder")
      .withStringItems(StringItems().addValues("abc", "xyz"))
    val parsedPbParam2 = PbParameter.parseFrom(pbParam2.toByteArray)

    pbParam2 shouldEqual parsedPbParam2
    parsedPbParam2 should not be param2
  }

  test("should able to create PbParameter with ArrayItems") {
    val array1 = Array(1000, 2000, 3000)
    val array2 = Array(-1, -2, -3)
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withIntArrayItems(IntArrayItems().addValues(array1, array2))

    val values = parameter.getIntArrayItems.values
    values.head shouldBe ArrayData.fromArray(array1)
    values.tail.head shouldBe ArrayData.fromArray(array2)
  }

  test("should able to create PbParameter with MatrixItems") {
    val matrixItems: IntMatrixItems = IntMatrixItems().set(
      Seq(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)), MatrixData.fromArrays(Array(11, 12, 13), Array(16, 17)))
    )
    matrixItems.values.head.values.head.head shouldBe 1
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntKey") {
    import spray.json.DefaultJsonProtocol._
    val key         = KeyType.IntKey.make("encoder")
    val param       = key.set(1, 2, 3, 4)
    val mapper      = Parameter.typeMapper[Int]
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntArrayKey") {
    val mapper      = Parameter.typeMapper2
    val key         = KeyType.IntArrayKey.make("blah")
    val param       = key.set(ArrayData.fromArray(1, 2, 3))
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntMatrixKey") {
    val mapper      = Parameter.typeMapper2
    val key         = KeyType.IntMatrixKey.make("blah")
    val param       = key.set(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)))
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
    mappedParam.keyType shouldBe IntMatrixKey
  }

  test("should able to change the type from/to PbParameter to/from Parameter using java api") {
    val key         = JKeyTypes.IntKey.make("encoder")
    val param       = key.set(1, 2, 3, 4)
    val mapper      = Parameter.typeMapper2
    val parsedParam = mapper.toCustom(mapper.toBase(param))

    parsedParam shouldEqual param
  }

  test("should able to change the type from/to PbParameter to/from Parameter for RaDec") {
    PbRaDec()
    PbRaDec.defaultInstance

    PbRaDec().withRa(10).withDec(32)
    PbRaDec().withRa(10).withDec(321)

    val raDec = RaDec(1, 2)
    val param = KeyType.RaDecKey.make("Asd").set(raDec)

    val pbParameter = Parameter.typeMapper2.toBase(param)
    pbParameter.toByteArray

    param.keyName shouldBe pbParameter.name
    param.units shouldBe pbParameter.units
    param.keyType shouldBe pbParameter.keyType
    param.items shouldBe pbParameter.getRaDecItems.values
  }
}
