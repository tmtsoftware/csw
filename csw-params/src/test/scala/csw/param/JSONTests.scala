package csw.param

import csw.param.Parameters.{CommandInfo, Observe, Setup, Wait}
import org.scalatest.FunSpec
import spray.json._
import ParameterSetJson._
import csw.param.Events.{ObserveEvent, StatusEvent, SystemEvent}
import csw.param.StateVariable.{CurrentState, DemandState}
import csw.param.UnitsOfMeasure.meters
import csw.param.parameters._
import csw.param.parameters.arrays._
import csw.param.parameters.matrices._
import csw.param.parameters.primitives._

object JSONTests extends DefaultJsonProtocol {

  // Example custom data type for a GenericItem
  case class MyData2(i: Int, f: Float, d: Double, s: String)

  //noinspection TypeAnnotation
  // Since automatic JSON reading doesn't work with generic types, we need to do it manually here
  case object MyData2 {
    // JSON read/write for MyData2
    implicit val myData2Format = jsonFormat4(MyData2.apply)

    // Creates a GenericItem[MyData2] from a JSON value (This didn't work with the jsonFormat3 method)
    def reader(json: JsValue): GParam[MyData2] = {
      json.asJsObject.getFields("keyName", "value", "units") match {
        case Seq(JsString(keyName), JsArray(v), u) =>
          val units = ParameterSetJson.unitsFormat.read(u)
          val value = v.map(MyData2.myData2Format.read)
          GParam[MyData2]("MyData2", keyName, value, units)
        case _ => throw DeserializationException("Invalid JSON for GenericItem[MyData2]")
      }
    }

    GParam.register("MyData2", reader)
  }
}

//noinspection ScalaUnusedSymbol
class JSONTests extends FunSpec {

  import JSONTests._

  private val s1: String = "encoder"
  private val s2: String = "filter"
  private val s3: String = "detectorTemp"

  private val ck = "wfos.blue.filter"

  private val commandInfo = CommandInfo(ObsId("Obs001"))

  describe("Test Subsystem JSON") {
    val wfos: Subsystem = Subsystem.WFOS

    it("should encode and decode properly") {
      val json = wfos.toJson
      //info("wfos: " + json)
      val sub = json.convertTo[Subsystem]
      assert(sub == wfos)
    }
  }

  describe("Test concrete items") {

    it("char item encode/decode") {
      val k1 = CharKey(s3)
      val i1 = k1.set('d').withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[CharParameter]
      assert(in1 == i1)
    }

    it("short item encode/decode") {
      val k1       = ShortKey(s3)
      val s: Short = -1
      val i1       = k1.set(s).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[ShortParameter]
      assert(in1 == i1)
    }

    it("int item encode/decode") {
      val k1 = IntKey(s3)
      val i1 = k1.set(23).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[IntParameter]
      assert(in1 == i1)
    }

    it("long item encode/decode") {
      val k1 = LongKey(s1)
      val i1 = k1.set(123456L).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[LongParameter]
      assert(in1 == i1)
    }

    it("float item encode/decode") {
      val k1 = FloatKey(s1)
      val i1 = k1.set(123.456f).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[FloatParameter]
      assert(in1 == i1)
    }

    it("double item encode/decode") {
      val k1 = DoubleKey(s1)
      val i1 = k1.set(123.456).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[DoubleParameter]
      assert(in1 == i1)
    }

    it("boolean item encode/decode") {
      val k1 = BooleanKey(s1)
      val i1 = k1.set(true, false).withUnits(UnitsOfMeasure.NoUnits)

      val j1 = i1.toJson
      //      info("j1: " + j1)
      val in1 = j1.convertTo[BooleanParameter]
      assert(in1 == i1)

      val i2 = k1.set(true)

      val j2  = i2.toJson
      val in2 = j2.convertTo[BooleanParameter]
      assert(in2 == i2)
    }

    it("string item encode/decode") {
      val k1 = StringKey(s2)
      val i1 = k1.set("Blue", "Green").withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[StringParameter]
      assert(in1 == i1)
    }
  }

  describe("Testing Items") {

    val k1 = IntKey(s1)
    val k2 = StringKey(s2)

    val i1 = k1.set(22, 33, 44)
    val i2 = k2.set("a", "b", "c").withUnits(UnitsOfMeasure.degrees)

    it("should encode and decode items list") {
      // Use this to get a list to test
      val sc1   = Setup(commandInfo, ck).add(i1).add(i2)
      val items = sc1.paramSet

      val js3 = ParameterSetJson.paramSetFormat.write(items)
      val in1 = ParameterSetJson.paramSetFormat.read(js3)
      assert(in1 == items)
    }
  }

  describe("Setup JSON") {

    val k1 = CharKey("a")
    val k2 = IntKey("b")
    val k3 = LongKey("c")
    val k4 = FloatKey("d")
    val k5 = DoubleKey("e")
    val k6 = BooleanKey("f")
    val k7 = StringKey("g")

    val i1 = k1.set('d').withUnits(UnitsOfMeasure.NoUnits)
    val i2 = k2.set(22).withUnits(UnitsOfMeasure.NoUnits)
    val i3 = k3.set(1234L).withUnits(UnitsOfMeasure.NoUnits)
    val i4 = k4.set(123.45f).withUnits(UnitsOfMeasure.degrees)
    val i5 = k5.set(123.456).withUnits(UnitsOfMeasure.meters)
    val i6 = k6.set(false)
    val i7 = k7.set("GG495").withUnits(UnitsOfMeasure.degrees)

    it("Should encode/decode a Setup") {
      val c1 = Setup(commandInfo, ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = ParameterSetJson.writeSequenceCommand(c1)
      val c1in  = ParameterSetJson.readSequenceCommand[Setup](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an Observe") {
      val c1 = Observe(commandInfo, ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = ParameterSetJson.writeSequenceCommand(c1)
      val c1in  = ParameterSetJson.readSequenceCommand[Observe](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an StatusEvent") {
      val e1 = StatusEvent(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = ParameterSetJson.writeEvent(e1)
      val e1in  = ParameterSetJson.readEvent[StatusEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in.info.eventTime == e1.info.eventTime)
      assert(e1in == e1)
    }

    it("Should encode/decode an ObserveEvent") {
      val e1 = ObserveEvent(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = ParameterSetJson.writeEvent(e1)
      val e1in  = ParameterSetJson.readEvent[ObserveEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an SystemEvent") {
      val e1 = SystemEvent(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = ParameterSetJson.writeEvent(e1)
      val e1in  = ParameterSetJson.readEvent[SystemEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an CurrentState") {
      val c1 = CurrentState(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = ParameterSetJson.writeStateVariable(c1)
      val c1in  = ParameterSetJson.readStateVariable[CurrentState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an DemandState") {
      val c1 = DemandState(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = ParameterSetJson.writeStateVariable(c1)
      val c1in  = ParameterSetJson.readStateVariable[DemandState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an Wait") {
      val c1 = Wait(commandInfo, ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = ParameterSetJson.writeSequenceCommand(c1)
      val c1in  = ParameterSetJson.readSequenceCommand[Wait](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }
  }

  describe("Test GenericItem") {
    it("Should allow a GenericItem with a custom type") {
      val k1  = GKey[MyData2]("MyData2", "testData")
      val d1  = MyData2(1, 2.0f, 3.0, "4")
      val d2  = MyData2(10, 20.0f, 30.0, "40")
      val i1  = k1.set(d1, d2).withUnits(UnitsOfMeasure.meters)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1.get(k1).get.values.size == 2)
      assert(sc1.get(k1).get.values(0) == d1)
      assert(sc1.get(k1).get.values(1) == d2)
      assert(sc1.get(k1).get.units == UnitsOfMeasure.meters)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("2: sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in.get(k1).get.values.size == 2)
      assert(sc1in.get(k1).get.values(0) == d1)
      assert(sc1in.get(k1).get.values(1) == d2)
      assert(sc1in.get(k1).get.units == UnitsOfMeasure.meters)

      val sc2 = Setup(commandInfo, ck).add(k1.set(d1, d2).withUnits(meters))
      assert(sc2 == sc1)
    }
  }

  describe("Test Custom RaDecItem") {
    it("Should allow cutom RaDecItem") {
      val k1  = GKey[RaDec]("RaDec", "coords")
      val c1  = RaDec(7.3, 12.1)
      val c2  = RaDec(9.1, 2.9)
      val i1  = k1.set(c1, c2)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1.get(k1).get.values.size == 2)
      assert(sc1.get(k1).get.values(0) == c1)
      assert(sc1.get(k1).get.values(1) == c2)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //        info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in.get(k1).get.values.size == 2)
      assert(sc1in.get(k1).get.values(0) == c1)
      assert(sc1in.get(k1).get.values(1) == c2)

      val sc2 = Setup(commandInfo, ck).add(k1.set(c1, c2))
      assert(sc2 == sc1)
    }
  }

  describe("Test Double Matrix items") {
    it("Should allow double matrix values") {
      val k1 = DoubleMatrixKey("myMatrix")
      val m1 = DoubleMatrix(
        Array(
          Array(1.0, 2.0, 3.0),
          Array(4.1, 5.1, 6.1),
          Array(7.2, 8.2, 9.2)
        )
      )
      val sc1 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Double Array items") {
    it("Should allow double array values") {
      val k1  = DoubleArrayKey("myArray")
      val m1  = DoubleArray(Array(1.0, 2.0, 3.0))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Int Matrix items") {
    it("Should allow int matrix values") {
      val k1  = IntMatrixKey("myMatrix")
      val m1  = IntMatrix(Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9)))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Int Array items") {
    it("Should allow int array values") {
      val k1  = IntArrayKey("myArray")
      val m1  = IntArray(Array(1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Byte Matrix items") {
    it("Should allow byte matrix values") {
      val k1  = ByteMatrixKey("myMatrix")
      val m1  = ByteMatrix(Array(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9)))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Byte Array items") {
    it("Should allow byte array values") {
      val k1  = ByteArrayKey("myArray")
      val m1  = ByteArray(Array[Byte](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Short Matrix items") {
    it("Should allow short matrix values") {
      val k1  = ShortMatrixKey("myMatrix")
      val m1  = ShortMatrix(Array.ofDim[Short](3, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)

      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Short Array items") {
    it("Should allow short array values") {
      val k1  = ShortArrayKey("myArray")
      val m1  = ShortArray(Array[Short](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Long Matrix items") {
    it("Should allow long matrix values") {
      val k1  = LongMatrixKey("myMatrix")
      val m1  = LongMatrix(Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9)))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Long Array items") {
    it("Should allow long array values") {
      val k1  = LongArrayKey("myArray")
      val m1  = LongArray(Array(1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Choice items") {
    it("Should allow choice/enum values") {
      val k1  = ChoiceKey("myChoice", Choices.from("A", "B", "C"))
      val c1  = Choice("B")
      val i1  = k1.set(c1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == c1)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(c1))
      assert(sc2 == sc1)
    }
  }

  /*describe("testing StructItem JSON support") {
    it("should allow Struct values") {
      val k1 = StructKey("myStruct")

      val ra    = StringKey("ra")
      val dec   = StringKey("dec")
      val epoch = DoubleKey("epoch")
      val c1    = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))
      val c2    = Struct().madd(ra.set("12:13:15.2"), dec.set("32:33:35.5"), epoch.set(1950.0))

      val i1: StructParameter = k1.set(c1, c2)

      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == c1)
      assert(sc1(k1).value(1) == c2)

      val sc1out = ParameterSetJson.writeSequenceCommand(sc1)

      val s = sc1out.prettyPrint
      println(s) // XXX

      val sc1in: Parameters.Setup = ParameterSetJson.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)
      //      val x = sc1in.get(k1).flatMap(_.head.get(ra))
      assert(sc1in(k1).head.get(ra).head.head == "12:13:14.1")

      //assert(sc1in(k1).value(1).name == "probe2")

      val sc2 = Setup(commandInfo, ck).add(k1.set(c1))
      assert(sc2 == sc1)
    }
  }
 */
}
