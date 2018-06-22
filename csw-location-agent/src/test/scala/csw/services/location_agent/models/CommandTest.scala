package csw.services.location_agent.models

import java.io.File
import java.nio.file.Paths

import org.scalatest.{FunSuite, Matchers}

class CommandTest extends FunSuite with Matchers {

  test("testParse with no options should return command with default values") {
    val opt        = Options()
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "false"
    c.port should not be 0
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }

  test("testParse with options should return executable command") {
    val opt        = Options(List("Alarm, Event, Telemetry"), Some("ls"), Some(8080), None, Some(9999), true)
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "ls"
    c.port shouldBe 8080
    c.delay shouldBe 9999
    c.noExit shouldBe true
  }

  test("testParse with config file should honour config options") {
    val url            = getClass.getResource("/redisTest.conf")
    val configFilePath = Paths.get(url.toURI).toFile.getAbsolutePath
    val configFile     = new File(configFilePath)
    val opt            = Options(List("redisTest"), None, None, Option(configFile))
    val c: Command     = Command.parse(opt)

    c.commandText shouldBe "redis-server --port 7777"
    c.port shouldBe 7777
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }

  test("testParse with config file but undefined value") {
    val url            = getClass.getResource("/redisTest.conf")
    val configFilePath = Paths.get(url.toURI).toFile.getAbsolutePath
    val configFile     = new File(configFilePath)
    val opt            = Options(List("redisTest-misSpelledKey"), None, None, Option(configFile))
    val c: Command     = Command.parse(opt)

    //due to mis-spelled key, false command is returned. which upon execution does nothing.
    c.commandText shouldBe "false"
  }

  test("testParse with config file port, command parameters are overridable from command line") {
    val url            = getClass.getResource("/redisTest.conf")
    val configFilePath = Paths.get(url.toURI).toFile.getAbsolutePath
    val configFile     = new File(configFilePath)
    val opt            = Options(List("redisTest"), Some("sleep"), Some(8888), Option(configFile))
    val c: Command     = Command.parse(opt)

    c.commandText shouldBe "sleep"
    c.port shouldBe 8888
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }

  test("testParse when empty command is specified, should execute a 'false' command") {
    val opt        = Options(List("redisTest"), None, Some(4444), None)
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "false"
    c.port shouldBe 4444
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }
}
