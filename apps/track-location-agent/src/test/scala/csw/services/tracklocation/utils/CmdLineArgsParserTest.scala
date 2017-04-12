package csw.services.tracklocation.utils

import csw.services.tracklocation.models.Options
import org.scalatest.{FunSuite, Matchers}

class CmdLineArgsParserTest
  extends FunSuite
  with Matchers {

  test("test parser with valid arguments") {
    val port = 5555
    val services = "redis,alarm,watchdog"
    val argv = Array("--name", services,
    "--port", port.toString,
    "--command", "sleep 5")

    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options(List("redis", "alarm", "watchdog"), Some("sleep 5"), Some(port), None, None, false))
  }

  test("test parser with invalid service name combinations") {
    val port = 5555
    val listOfInvalidServices: List[String] = List("re-dis,alarm", "redis, alarm", " redis,alarm-service", "redis, alarm ", "redis, ,alarm")

    listOfInvalidServices.foreach{ service =>
      val argv = Array("--name", service,
        "--port", port.toString,
        "--command", "sleep 5")
      CmdLineArgsParser.parser.parse(argv, Options()) shouldEqual None
    }
  }

  test("test parser with only --name option, should be allowed") {
    val argv = Array[String]("--name", "myService")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())

    x should contain (new Options(List("myService"), None, None, None, None, false))
  }

  test("test parser without --name option, should error out") {
    val argv = Array[String]("abcd")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())

    x shouldEqual None
  }

  test("test parser with service name containing '-' character, should error out") {
    val argv = Array[String]("--name", "alarm-service")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test parser with list of services containing leading/trailing whitespace, should error out") {
    val services = "redis,alarm,watchdog"
    val argv = Array[String]("--name", "   redis-server,   alarm,watchdog   ")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())

    x shouldEqual None
  }

  test("test parser with service name containing leading whitespace, should error out") {
    val argv = Array[String]("--name", " someService")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())

    x shouldEqual None
  }

  test("test parser with service name containing trailing whitespace, should error out") {
    val argv = Array[String]("--name", "someService ")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())

    x shouldEqual None
  }

  test("test parser with service name containing both leading and trailing whitespaces, error is shown") {
    val argv = Array[String]("--name", " someService ")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())

    x shouldEqual None
  }
}