package csw.messages.params.formats

import csw.messages.commands._
import csw.messages.events.{EventName, ObserveEvent, SystemEvent}
import csw.messages.params.formats.JsonSupport._
import csw.messages.params.generics.KeyType.{
  ByteMatrixKey,
  ChoiceKey,
  DoubleKey,
  DoubleMatrixKey,
  IntMatrixKey,
  LongMatrixKey,
  ShortMatrixKey,
  StringKey,
  StructKey
}
import csw.messages.params.generics._
import csw.messages.params.models.Units.{degree, encoder, meter, NoUnits}
import csw.messages.params.models._
import csw.messages.params.states.{CurrentState, DemandState, StateName}
import org.scalatest.FunSpec
import play.api.libs.json.Json

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-188: Efficient Serialization to/from JSON
class JsonTest extends FunSpec {

  private val s1: String = "encoder"
  private val s2: String = "filter"
  private val s3: String = "detectorTemp"

  private val ck = "wfos.blue.filter"

  private val obsId: ObsId = ObsId("Obs001")

  describe("Test Subsystem JSON") {
    val wfos: Subsystem = Subsystem.WFOS

    it("should encode and decode properly") {
      val expectedJson = Json.parse("\"wfos\"")
      val json         = Json.toJson(wfos)
      val sub          = json.as[Subsystem]
      assert(sub == wfos)
      assert(json.equals(expectedJson))
    }
  }

  describe("Test Units JSON") {
    val encoderUnit: Units = encoder

    it("should encode and decode properly") {
      val json                 = Json.toJson(encoderUnit)
      val encoderUnitsFromJson = json.as[Units]
      assert(encoderUnit == encoderUnitsFromJson)
    }
  }

  describe("Test concrete items") {

    it("char item encode/decode without units") {
      val k1 = KeyType.CharKey.make(s3)
      val i1 = k1.set('d').withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Char]]
      assert(in1 == i1)
      assert(in1.units == i1.units)
      assert(in1.units == NoUnits)
    }

    it("char item encode/decode with units") {
      val k1 = KeyType.CharKey.make(s3)
      val i1 = k1.set('d').withUnits(encoder)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Char]]
      assert(in1 == i1)
      assert(in1.units == i1.units)
      assert(in1.units == encoder)
    }

    it("short item encode/decode") {
      val k1       = KeyType.ShortKey.make(s3)
      val s: Short = -1
      val i1       = k1.set(s).withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Short]]
      assert(in1 == i1)
    }

    it("int item encode/decode") {
      val k1 = KeyType.IntKey.make(s3)
      val i1 = k1.set(23).withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Int]]
      assert(in1 == i1)
    }

    it("long item encode/decode") {
      val k1 = KeyType.LongKey.make(s1)
      val i1 = k1.set(123456L).withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Long]]
      assert(in1 == i1)
    }

    it("float item encode/decode") {
      val k1 = KeyType.FloatKey.make(s1)
      val i1 = k1.set(123.456f).withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Float]]
      assert(in1 == i1)
    }

    it("double item encode/decode") {
      val k1 = KeyType.DoubleKey.make(s1)
      val i1 = k1.set(123.456).withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[Double]]
      assert(in1 == i1)
    }

    it("boolean item encode/decode") {
      val k1 = KeyType.BooleanKey.make(s1)
      val i1 = k1.set(true, false).withUnits(NoUnits)

      val j1 = i1.toJson
      //      info("j1: " + j1)
      val in1: Parameter[Boolean] = j1.as[Parameter[Boolean]]
      assert(in1 == i1)

      val i2 = k1.set(true)

      val j2  = i2.toJson
      val in2 = j2.as[Parameter[Boolean]]
      assert(in2 == i2)
    }

    it("string item encode/decode") {
      val k1 = KeyType.StringKey.make(s2)
      val i1 = k1.set("Blue", "Green").withUnits(NoUnits)

      val j1  = i1.toJson
      val in1 = j1.as[Parameter[String]]
      assert(in1 == i1)
    }
  }

  describe("Testing Items") {

    val k1 = KeyType.IntKey.make(s1)
    val k2 = KeyType.StringKey.make(s2)

    val i1 = k1.set(22, 33, 44)
    val i2 = k2.set("a", "b", "c").withUnits(degree)

    it("should encode and decode items list") {
      // Use this to get a list to test
      val sc1   = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1).add(i2)
      val items = sc1.paramSet

      val js3 = JsonSupport.paramSetFormat.writes(items)
      val in1 = JsonSupport.paramSetFormat.reads(js3)
      assert(in1.get == items)
    }
  }

  describe("Commands and Events JSON") {

    val k1 = KeyType.CharKey.make("a")
    val k2 = KeyType.IntKey.make("b")
    val k3 = KeyType.LongKey.make("c")
    val k4 = KeyType.FloatKey.make("d")
    val k5 = KeyType.DoubleKey.make("e")
    val k6 = KeyType.BooleanKey.make("f")
    val k7 = KeyType.StringKey.make("g")

    val i1 = k1.set('d').withUnits(NoUnits)
    val i2 = k2.set(22).withUnits(NoUnits)
    val i3 = k3.set(1234L).withUnits(NoUnits)
    val i4 = k4.set(123.45f).withUnits(degree)
    val i5 = k5.set(123.456).withUnits(meter)
    val i6 = k6.set(false)
    val i7 = k7.set("GG495").withUnits(degree)

    it("Should encode/decode a Setup") {
      val c1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Setup](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    // DEOPSCSW-315: Make ObsID optional in commands
    it("Should encode/decode an Observe") {
      val c1 = Observe(Prefix(ck), CommandName("move"), Some(obsId)).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Observe](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
      assert(c1in.maybeObsId.contains(obsId))
    }

    // DEOPSCSW-315: Make ObsID optional in commands
    it("Should encode/decode an Wait") {
      val c1 = Wait(Prefix(ck), CommandName("move"), None).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Wait](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
      assert(c1in.maybeObsId.isEmpty)
    }

    it("Should encode/decode an ObserveEvent") {
      val e1 = ObserveEvent(ck, EventName("")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[ObserveEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an SystemEvent") {
      val e1 = SystemEvent(ck, EventName("")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[SystemEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an CurrentState") {
      val c1 = CurrentState(Prefix(ck), StateName("testStateName")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeStateVariable(c1)
      val c1in  = JsonSupport.readStateVariable[CurrentState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an DemandState") {
      val c1 = DemandState(Prefix(ck), StateName("testStateName")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeStateVariable(c1)
      val c1in  = JsonSupport.readStateVariable[DemandState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }
  }

  describe("Test Custom RaDecItem") {
    it("Should allow custom RaDecItem") {
      val k1  = KeyType.RaDecKey.make("coords")
      val c1  = RaDec(7.3, 12.1)
      val c2  = RaDec(9.1, 2.9)
      val i1  = k1.set(c1, c2)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1.get(k1).get.values.size == 2)
      assert(sc1.get(k1).get.values(0) == c1)
      assert(sc1.get(k1).get.values(1) == c2)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //        info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in.get(k1).get.values.size == 2)
      assert(sc1in.get(k1).get.values(0) == c1)
      assert(sc1in.get(k1).get.values(1) == c2)
    }
  }

  describe("Test Double Matrix items") {
    it("Should allow double matrix values") {
      val k1  = DoubleMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array(1.0, 2.0, 3.0), Array(4.1, 5.1, 6.1), Array(7.2, 8.2, 9.2))
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(k1.set(m1))
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

    }
  }

  describe("Test Double Array items") {
    it("Should allow double array values") {
      val k1  = KeyType.DoubleArrayKey.make("myArray")
      val m1  = ArrayData(Array(1.0, 2.0, 3.0))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Int Matrix items") {
    it("Should allow int matrix values") {
      val k1  = IntMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Int Array items") {
    it("Should allow int array values") {
      val k1  = KeyType.IntArrayKey.make("myArray")
      val m1  = ArrayData(Array(1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  // DEOPSCSW-186: Binary value payload
  describe("Test Byte Matrix items") {
    it("Should allow byte matrix values") {
      val k1  = ByteMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  // DEOPSCSW-186: Binary value payload
  describe("Test Byte Array items") {
    it("Should allow byte array values") {
      val k1  = KeyType.ByteArrayKey.make("myArray")
      val m1  = ArrayData(Array[Byte](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Short Matrix items") {
    it("Should allow short matrix values") {
      val k1  = ShortMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array.ofDim[Short](3, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)

      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Short Array items") {
    it("Should allow short array values") {
      val k1  = KeyType.ShortArrayKey.make("myArray")
      val m1  = ArrayData(Array[Short](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Long Matrix items") {
    it("Should allow long matrix values") {
      val k1                   = LongMatrixKey.make("myMatrix")
      val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
      val i1                   = k1.set(m1)
      val sc1                  = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Long Array items") {
    it("Should allow long array values") {
      val k1: Key[ArrayData[Long]]       = KeyType.LongArrayKey.make("myArray")
      val m1: ArrayData[Long]            = ArrayData(Array(1, 2, 3))
      val i1: Parameter[ArrayData[Long]] = k1.set(m1)
      val sc1                            = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Choice items") {
    it("Should allow choice/enum values") {
      val k1  = ChoiceKey.make("myChoice", Choices.from("A", "B", "C"))
      val c1  = Choice("B")
      val i1  = k1.set(c1)
      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == c1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)
    }
  }

  describe("testing StructItem JSON support") {
    it("should allow Struct values") {
      val k1    = StructKey.make("myStruct")
      val ra    = StringKey.make("ra")
      val dec   = StringKey.make("dec")
      val epoch = DoubleKey.make("epoch")
      val c1    = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))
      val c2    = Struct().madd(ra.set("12:13:15.2"), dec.set("32:33:35.5"), epoch.set(1950.0))

      val i1: Parameter[Struct] = k1.set(c1, c2)

      val sc1 = Setup(Prefix(ck), CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == c1)
      assert(sc1(k1).value(1) == c2)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in: Setup = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)
      assert(sc1in(k1).head.get(ra).head.head == "12:13:14.1")
    }
  }
}
