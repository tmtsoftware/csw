/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.agent.models

import csw.commons.ResourceReader
import csw.location.agent.args.Options
import csw.prefix.models.Prefix
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CommandTest extends AnyFunSuite with Matchers {

  test("testParse with no options should return command with default values") {
    val opt        = Options()
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "false"
    c.port should not be 0
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }

  test("testParse with options should return executable command") {
    val opt =
      Options(
        List("csw.Alarm, csw.Event, csw.Telemetry").map(Prefix(_)),
        Some("ls"),
        Some(8080),
        delay = Some(9999),
        noExit = true
      )
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "ls"
    c.port shouldBe 8080
    c.delay shouldBe 9999
    c.noExit shouldBe true
  }

  test("testParse with config file should honour config options") {
    val configFile = ResourceReader.copyToTmp("/redisTest.conf").toFile
    val opt        = Options(List(Prefix("csw.redisTest")), None, None, appConfigFile = Option(configFile))
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "redis-server --port 7777"
    c.port shouldBe 7777
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }

  test("testParse with config file but undefined value") {
    val configFile = ResourceReader.copyToTmp("/redisTest.conf").toFile
    val opt        = Options(List(Prefix("csw.redisTest_misSpelledKey")), appConfigFile = Option(configFile))
    val c: Command = Command.parse(opt)

    // due to mis-spelled key, false command is returned. which upon execution does nothing.
    c.commandText shouldBe "false"
  }

  test("testParse with config file port, command parameters are overridable from command line") {
    val configFile = ResourceReader.copyToTmp("/redisTest.conf").toFile
    val opt        = Options(List(Prefix("csw.redisTest")), Some("sleep"), Some(8888), appConfigFile = Option(configFile))
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "sleep"
    c.port shouldBe 8888
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }

  test("testParse when empty command is specified, should execute a 'false' command") {
    val opt        = Options(List(Prefix("csw.redisTest")), None, Some(4444), None)
    val c: Command = Command.parse(opt)

    c.commandText shouldBe "false"
    c.port shouldBe 4444
    c.delay shouldBe Command.defaultDelay
    c.noExit shouldBe false
  }
}
