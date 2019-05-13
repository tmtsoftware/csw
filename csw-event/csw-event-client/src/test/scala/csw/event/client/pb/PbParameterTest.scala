package csw.event.client.pb

import csw.params.core.generics.KeyType
import csw.params.core.generics.KeyType.IntMatrixKey
import csw.params.core.models._
import csw.params.javadsl.JKeyType
import csw.time.core.models.{TAITime, UTCTime}
import csw_protobuf.models._
import csw_protobuf.parameter._
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-297: Merge protobuf branch in master
class PbParameterTest extends FunSuite with Matchers {

  test("should able to parse PbParameter with multiple values") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.centimeter)
      .withItems(IntItems().addValues(1, 2))

    val parsedPbParam: PbParameter = PbParameter.parseFrom(parameter.toByteArray)

    parameter shouldBe parsedPbParam
  }

  test("should able to parse PbParameter with sequence of values") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withItems(IntItems().withValues(Seq(1, 2, 3, 4)))

    val parsedParameter = PbParameter.parseFrom(parameter.toByteArray)

    parameter shouldEqual parsedParameter
  }

  test("should able to create Boolean Items") {
    val booleanItems: BooleanItems = BooleanItems().addValues(true).addValues(false)
    val parsedBooleanItems         = BooleanItems.parseFrom(booleanItems.toByteArray)
    booleanItems.values shouldBe parsedBooleanItems.values
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to create PbParameter with UTCTime items") {
    val items = UTCTimeItems(Seq(UTCTime.now()))
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.UTCTimeKey)
      .withItems(items)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.UTCTimeKey
    parameter.items shouldBe items
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to create PbParameter with TAITime items") {
    val items = TAITimeItems(Seq(TAITime.now()))
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.TAITimeKey)
      .withItems(items)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.TAITimeKey
    parameter.items shouldBe items
  }

  test("should able to create PbParameter with Byte items") {
    val items = ByteItems(Seq(1, 2, 3, 4))
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.UTCTimeKey)
      .withItems(items)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.UTCTimeKey
    parameter.items shouldBe items
  }

  test("should able to create PbParameter with Choice items") {
    val choices = ChoiceItems().set(Seq("a345", "b234", "c567", "d890"))
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.UTCTimeKey)
      .withItems(choices)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.UTCTimeKey
  }

  test("should able to create PbParameter with int items only when KeyType is Int") {
    val intItems = IntItems().addValues(1)
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.IntKey)
      .withItems(CharItems().set(Seq('a', 'b')))
      .withItems(intItems)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.IntKey
    parameter.items shouldBe intItems
  }

  test("should able to create PbParameter and compare with Parameter for Int and String Key") {
    val key   = KeyType.IntKey.make("encoder")
    val param = key.set(1, 2, 3, 4)

    val items = IntItems().addValues(1, 2, 3, 4)
    val pbParam = PbParameter()
      .withName("encoder")
      .withUnits(Units.angstrom)
      .withItems(items)

    items.values shouldBe Seq(1, 2, 3, 4)

    val parsedPbParam = PbParameter.parseFrom(pbParam.toByteArray)

    pbParam shouldBe parsedPbParam
    parsedPbParam should not be param

    val key2   = KeyType.StringKey.make("encoder")
    val param2 = key2.set("abc", "xyc")

    val pbParam2 = PbParameter()
      .withName("encoder")
      .withItems(StringItems().addValues("abc", "xyz"))
    val parsedPbParam2 = PbParameter.parseFrom(pbParam2.toByteArray)

    pbParam2 shouldEqual parsedPbParam2
    parsedPbParam2 should not be param2
  }

  test("should able to create PbParameter with ArrayItems") {
    val array1 = Array(1000, 2000, 3000)
    val array2 = Array(-1, -2, -3)
    val items  = IntArrayItems().addValues(array1, array2)
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withItems(items)

    val intArrayItems = parameter.items.asInstanceOf[IntArrayItems]
    intArrayItems.values.head shouldBe ArrayData.fromArray(array1)
    intArrayItems.values.tail.head shouldBe ArrayData.fromArray(array2)
  }

  test("should able to create PbParameter with MatrixItems") {
    val matrixItems: IntMatrixItems = IntMatrixItems().set(
      Seq(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)), MatrixData.fromArrays(Array(11, 12, 13), Array(16, 17)))
    )
    matrixItems.values.head.values.head.head shouldBe 1
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntKey") {
    val key         = KeyType.IntKey.make("encoder")
    val param       = key.set(1, 2, 3, 4)
    val mapper      = TypeMapperSupport.parameterTypeMapper2
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to change the type from/to PbParameter to/from Parameter for UTCTimeKey") {
    val mapper      = TypeMapperSupport.parameterTypeMapper2
    val key         = KeyType.UTCTimeKey.make("utcTimeKey")
    val param       = key.set(UTCTime.now())
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to change the type from/to PbParameter to/from Parameter for TAITimeKey") {
    val mapper      = TypeMapperSupport.parameterTypeMapper2
    val key         = KeyType.TAITimeKey.make("taiTimeKey")
    val param       = key.set(TAITime.now())
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntArrayKey") {
    val mapper      = TypeMapperSupport.parameterTypeMapper2
    val key         = KeyType.IntArrayKey.make("blah")
    val param       = key.set(ArrayData.fromArray(1, 2, 3))
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntMatrixKey") {
    val mapper      = TypeMapperSupport.parameterTypeMapper2
    val key         = KeyType.IntMatrixKey.make("blah")
    val param       = key.set(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)))
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
    mappedParam.keyType shouldBe IntMatrixKey
  }

  test("should able to change the type from/to PbParameter to/from Parameter using java api") {
    val key         = JKeyType.IntKey.make("encoder")
    val param       = key.set(1, 2, 3, 4)
    val mapper      = TypeMapperSupport.parameterTypeMapper2
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

    val pbParameter = TypeMapperSupport.parameterTypeMapper2.toBase(param)
    pbParameter.toByteArray

    param.keyName shouldBe pbParameter.name
    param.units shouldBe pbParameter.units
    param.keyType shouldBe pbParameter.keyType
    param.items shouldBe pbParameter.items.asMessage.getRaDecItems.values
  }
}
