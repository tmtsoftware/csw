package csw.services.event.cli.args

import csw.messages.params.generics.KeyType.{BooleanKey, DoubleKey, FloatKey, IntKey, LongKey, StringKey}
import csw.messages.params.models.Units.{centimeter, meter, volt}
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-436: [Event Cli] Specialized Publish command (take params from command line)
class ParameterArgParserTest extends FunSuite with Matchers {

  test("should able to parse valid single param string when key name, type, units & values provided") {
    ParameterArgParser.parse("k1:i:meter=[1,2,3]|k2:d=10.11,20.22") shouldBe
    Set(IntKey.make("k1").set(1, 2, 3).withUnits(meter), DoubleKey.make("k2").set(10.11, 20.22))
  }

  test("should able to parse valid string params") {
    ParameterArgParser.parse("k1:s=['a, b','cd']|k2:s=['x,y','z']") shouldBe
    Set(StringKey.make("k1").set("a, b", "cd"), StringKey.make("k2").set("x,y", "z"))
  }

  test("should able to parse valid string params with single quote and comma") {
    ParameterArgParser.parse("k1:s=['Kevin O\\'Brien','Chicago, USA']|k2:s=['2016-08-05T16:23:19.002']") shouldBe
    Set(StringKey.make("k1").set("Kevin O'Brien", "Chicago, USA"), StringKey.make("k2").set("2016-08-05T16:23:19.002"))
  }

  test("should able to parse all valid space separated multi param string") {
    val allParamsStr = "k1:i=[1]|k2:s:volt=[5v]|k3:f=[2.0]|k4:d:centimeter=[5.0]|k5:l=[10,20]|k6:b=[true]"
    val p1           = IntKey.make("k1").set(1)
    val p2           = StringKey.make("k2").set("5v").withUnits(volt)
    val p3           = FloatKey.make("k3").set(2.0f)
    val p4           = DoubleKey.make("k4").set(5.0).withUnits(centimeter)
    val p5           = LongKey.make("k5").set(10, 20)
    val p6           = BooleanKey.make("k6").set(true)

    ParameterArgParser.parse(allParamsStr) shouldBe Set(p1, p2, p3, p4, p5, p6)
  }

  test("should throw exception when values are not provided for some of the param") {
    intercept[RuntimeException](ParameterArgParser.parse("k:i=1|k2:s"))
  }

  test("should throw exception when unsupported key type provided") {
    intercept[RuntimeException](ParameterArgParser.parse("k:x:volt=1v"))
  }

  test("should throw exception when invalid unit provided") {
    intercept[RuntimeException](ParameterArgParser.parse("k:s:invalid=1v"))
  }
}
