package csw.params.core.formats

import csw.params.commands._
import csw.params.core.generics.KeyType._
import csw.params.core.generics._
import csw.params.core.models.Coords.EqFrame.FK5
import csw.params.core.models.Coords.SolarSystemObject.Venus
import csw.params.core.models.Units.{NoUnits, degree, encoder, meter}
import csw.params.core.models._
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.funspec.AnyFunSpec
import play.api.libs.json.Json

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-188: Efficient Serialization to/from JSON
class JsonTest extends AnyFunSpec {

  import ParamCodecs._

  private val s1: String = "encoder"
  private val s2: String = "filter"
  private val s3: String = "detectorTemp"

  private val ck = Prefix("wfos.blue.filter")

  private val obsId: ObsId = ObsId("2020A-001-123")

  describe("Test Subsystem JSON") {
    val wfos: Subsystem = Subsystem.WFOS

    // CSW-86: Subsystem should be case-insensitive
    it("should encode and decode properly | DEOPSCSW-183, DEOPSCSW-188") {
      val expectedJson = Json.parse("\"WFOS\"")
      val json         = JsonSupport.writes(wfos)
      val sub          = JsonSupport.reads[Subsystem](json)
      assert(sub == wfos)
      assert(json.equals(expectedJson))
    }
  }

  describe("Test Prefix") {
    val prefix: Prefix = Prefix("wfos.filter.wheel")

    // CSW-86: Subsystem should be case-insensitive
    it("should encode and decode properly | DEOPSCSW-183, DEOPSCSW-188") {
      val expectedJson = Json.parse("\"WFOS.filter.wheel\"")
      val json         = JsonSupport.writes(prefix)
      val sub          = JsonSupport.reads[Prefix](json)
      assert(sub == prefix)
      assert(json.equals(expectedJson))
    }
  }

  describe("Test Units JSON") {
    val encoderUnit: Units = encoder

    it("should encode and decode properly | DEOPSCSW-183, DEOPSCSW-188") {
      val json                 = JsonSupport.writes(encoderUnit)
      val encoderUnitsFromJson = JsonSupport.reads[Units](json)
      assert(encoderUnit == encoderUnitsFromJson)
    }
  }

  describe("Test concrete items") {

    it("char item encode/decode without units | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.CharKey.make(s3)
      val i1 = k1.set('d').withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Char]](j1)
      assert(in1 == i1)
      assert(in1.units == i1.units)
      assert(in1.units == NoUnits)
    }

    it("char item encode/decode with units | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.CharKey.make(s3)
      val i1 = k1.set('d').withUnits(encoder)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Char]](j1)
      assert(in1 == i1)
      assert(in1.units == i1.units)
      assert(in1.units == encoder)
    }

    it("short item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1       = KeyType.ShortKey.make(s3)
      val s: Short = -1
      val i1       = k1.set(s).withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Short]](j1)
      assert(in1 == i1)
    }

    it("int item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.IntKey.make(s3)
      val i1 = k1.set(23).withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Int]](j1)
      assert(in1 == i1)
    }

    it("long item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.LongKey.make(s1)
      val i1 = k1.set(123456L).withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Long]](j1)
      assert(in1 == i1)
    }

    it("float item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.FloatKey.make(s1)
      val i1 = k1.set(123.456f).withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Float]](j1)
      assert(in1 == i1)
    }

    it("double item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.DoubleKey.make(s1)
      val i1 = k1.set(123.456).withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[Double]](j1)
      assert(in1 == i1)
    }

    it("boolean item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.BooleanKey.make(s1)
      val i1 = k1.set(true, false).withUnits(NoUnits)

      val j1 = JsonSupport.writes(i1)
      //      info("j1: " + j1)
      val in1: Parameter[Boolean] = JsonSupport.reads[Parameter[Boolean]](j1)
      assert(in1 == i1)

      val i2 = k1.set(true)

      val j2  = JsonSupport.writes(i2)
      val in2 = JsonSupport.reads[Parameter[Boolean]](j2)
      assert(in2 == i2)
    }

    it("string item encode/decode | DEOPSCSW-183, DEOPSCSW-188") {
      val k1 = KeyType.StringKey.make(s2)
      val i1 = k1.set("Blue", "Green").withUnits(NoUnits)

      val j1  = JsonSupport.writes(i1)
      val in1 = JsonSupport.reads[Parameter[String]](j1)
      assert(in1 == i1)
    }
  }

  describe("Testing Items") {

    val k1 = KeyType.IntKey.make(s1)
    val k2 = KeyType.StringKey.make(s2)

    val i1 = k1.set(22, 33, 44)
    val i2 = k2.set("a", "b", "c").withUnits(degree)

    it("should encode and decode items list | DEOPSCSW-183, DEOPSCSW-188") {
      // Use this to get a list to test
      val sc1   = Setup(ck, CommandName("move"), Some(obsId)).add(i1).add(i2)
      val items = sc1.paramSet

      val js3 = JsonSupport.writes(items)
      val in1 = JsonSupport.reads[Set[Parameter[_]]](js3)
      assert(in1 == items)
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

    it("Should encode/decode a Setup | DEOPSCSW-183, DEOPSCSW-188") {
      val c1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Setup](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    // DEOPSCSW-315: Make ObsID optional in commands
    it("Should encode/decode an Observe | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-315") {
      val c1 = Observe(ck, CommandName("move"), Some(obsId)).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Observe](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
      assert(c1in.maybeObsId.contains(obsId))
    }

    // DEOPSCSW-315: Make ObsID optional in commands
    it("Should encode/decode an Wait | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-315") {
      val c1 = Wait(ck, CommandName("move"), None).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Wait](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
      assert(c1in.maybeObsId.isEmpty)
    }

    it("Should encode/decode an ObserveEvent | DEOPSCSW-183, DEOPSCSW-188") {
      val e1 = ObserveEvent(ck, EventName("")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[ObserveEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an SystemEvent | DEOPSCSW-183, DEOPSCSW-188") {
      val e1 = SystemEvent(ck, EventName("")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[SystemEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an CurrentState | DEOPSCSW-183, DEOPSCSW-188") {
      val c1 = CurrentState(ck, StateName("testStateName")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeStateVariable(c1)
      val c1in  = JsonSupport.readStateVariable[CurrentState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an DemandState | DEOPSCSW-183, DEOPSCSW-188") {
      val c1 = DemandState(ck, StateName("testStateName")).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeStateVariable(c1)
      val c1in  = JsonSupport.readStateVariable[DemandState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }
  }

  describe("Test Coordinate Types") {
    import Angle._
    import Coords._
    it("Should allow coordinate types | DEOPSCSW-183, DEOPSCSW-188") {
      val basePosKey       = CoordKey.make("BasePosition")
      val pm               = ProperMotion(0.5, 2.33)
      val eqCoord          = EqCoord(ra = "12:13:14.15", dec = "-30:31:32.3", frame = FK5, pmx = pm.pmx, pmy = pm.pmy)
      val solarSystemCoord = SolarSystemCoord(Tag("BASE"), Venus)
      val minorPlanetCoord = MinorPlanetCoord(Tag("GUIDER1"), 2000, 90.degree, 2.degree, 100.degree, 1.4, 0.234, 220.degree)
      val cometCoord       = CometCoord(Tag("BASE"), 2000.0, 90.degree, 2.degree, 100.degree, 1.4, 0.234)
      val altAzCoord       = AltAzCoord(Tag("BASE"), 301.degree, 42.5.degree)
      val posParam         = basePosKey.set(eqCoord, solarSystemCoord, minorPlanetCoord, cometCoord, altAzCoord)

      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(posParam)
      assert(sc1.get(basePosKey).get.values.length == 5)
      assert(sc1.get(basePosKey).get.values(0) == eqCoord)
      assert(sc1.get(basePosKey).get.values(1) == solarSystemCoord)
      assert(sc1.get(basePosKey).get.values(2) == minorPlanetCoord)
      assert(sc1.get(basePosKey).get.values(3) == cometCoord)
      assert(sc1.get(basePosKey).get.values(4) == altAzCoord)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //        info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1.get(basePosKey).get.values.length == 5)
      assert(sc1.get(basePosKey).get.values(0) == eqCoord)
      assert(sc1.get(basePosKey).get.values(1) == solarSystemCoord)
      assert(sc1.get(basePosKey).get.values(2) == minorPlanetCoord)
      assert(sc1.get(basePosKey).get.values(3) == cometCoord)
      assert(sc1.get(basePosKey).get.values(4) == altAzCoord)
    }
  }

  describe("Test Double Matrix items") {
    it("Should allow double matrix values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = DoubleMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array(1.0, 2.0, 3.0), Array(4.1, 5.1, 6.1), Array(7.2, 8.2, 9.2))
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(k1.set(m1))
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

    }
  }

  describe("Test Double Array items") {
    it("Should allow double array values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = KeyType.DoubleArrayKey.make("myArray")
      val m1  = ArrayData(Array(1.0, 2.0, 3.0))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Int Matrix items") {
    it("Should allow int matrix values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = IntMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      // info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Int Array items") {
    it("Should allow int array values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = KeyType.IntArrayKey.make("myArray")
      val m1  = ArrayData(Array(1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      // info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  // DEOPSCSW-186: Binary value payload
  describe("Test Byte Matrix items") {
    it("Should allow byte matrix values | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-186") {
      val k1  = ByteMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  // DEOPSCSW-186: Binary value payload
  describe("Test Byte Array items") {
    it("Should allow byte array values | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-186") {
      val k1  = KeyType.ByteArrayKey.make("myArray")
      val m1  = ArrayData(Array[Byte](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Short Matrix items") {
    it("Should allow short matrix values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = ShortMatrixKey.make("myMatrix")
      val m1  = MatrixData.fromArrays(Array.ofDim[Short](3, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)

      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Short Array items") {
    it("Should allow short array values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = KeyType.ShortArrayKey.make("myArray")
      val m1  = ArrayData(Array[Short](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Long Matrix items") {
    it("Should allow long matrix values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1                   = LongMatrixKey.make("myMatrix")
      val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
      val i1                   = k1.set(m1)
      val sc1                  = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Long Array items") {
    it("Should allow long array values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1: Key[ArrayData[Long]]       = KeyType.LongArrayKey.make("myArray")
      val m1: ArrayData[Long]            = ArrayData(Array(1L, 2L, 3L))
      val i1: Parameter[ArrayData[Long]] = k1.set(m1)
      val sc1                            = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)
    }
  }

  describe("Test Choice items") {
    it("Should allow choice/enum values | DEOPSCSW-183, DEOPSCSW-188") {
      val k1  = ChoiceKey.make("myChoice", Choices.from("A", "B", "C"))
      val c1  = Choice("B")
      val i1  = k1.set(c1)
      val sc1 = Setup(ck, CommandName("move"), Some(obsId)).add(i1)
      assert(sc1(k1).head == c1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)
    }
  }

}
