package csw.services.config.server.cli
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-130: Command line App for HTTP server
class ArgsParserTest extends FunSuite with Matchers {

  test("should set init to false and port to None if no options are provided") {
    val args               = Array[String]()
    val x: Option[Options] = new ArgsParser().parse(args)
    x.get shouldEqual Options(initRepo = false, None)
  }

  test("should set init to true if option --initRepo is provided") {
    val args               = Array("--initRepo")
    val x: Option[Options] = new ArgsParser().parse(args)
    x.get shouldEqual Options(initRepo = true, None)
  }

  test("should set port with the value provided with -- port option") {
    val args               = Array("--port", "2345")
    val x: Option[Options] = new ArgsParser().parse(args)
    x.get shouldEqual Options(initRepo = false, Some(2345))
  }

  test("should set init to true if --initRepo option is provided and port with the value provided with -- port option") {
    val args               = Array("--initRepo", "--port", "2345")
    val x: Option[Options] = new ArgsParser().parse(args)
    x.get shouldEqual Options(initRepo = true, Some(2345))
  }
}
