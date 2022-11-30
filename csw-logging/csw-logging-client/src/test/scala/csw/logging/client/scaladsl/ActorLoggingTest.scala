/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.scaladsl

import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.commons.LoggingKeys
import csw.logging.client.components.IRIS
import csw.logging.client.components.IRISLogMessages._
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.utils.LoggingTestSuite
import csw.logging.models.Level
import csw.logging.models.Level.ERROR
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

class ActorLoggingTest extends LoggingTestSuite {
  private val prefix: Prefix = Prefix(CSW, IRIS.COMPONENT_NAME)
  private val irisActorRef =
    actorSystem.spawn(IRIS.behavior(prefix), name = "IRIS-Supervisor-Actor")

  def sendMessagesToActor(): Unit = {
    irisActorRef ! LogTrace
    irisActorRef ! LogDebug
    irisActorRef ! LogInfo
    irisActorRef ! LogWarn
    irisActorRef ! LogError
    irisActorRef ! LogFatal
    irisActorRef ! LogErrorWithMap("Unknown")
    Thread.sleep(300)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  // DEOPSCSW-158: Logging service API implementation details to be hidden from component developer
  // CSW-78: PrefixRedesign for logging
  test(
    "messages logged from actor should contain component name, file name, class name, line number and actor path | DEOPSCSW-121, DEOPSCSW-117, DEOPSCSW-116, DEOPSCSW-158, DEOPSCSW-119"
  ) {

    sendMessagesToActor()

    // default log level for IrisSupervisorActor is ERROR in config
    var logMsgLineNumber = IRIS.ERROR_LINE_NO

    logBuffer.foreach { log =>
      log.getString(LoggingKeys.COMPONENT_NAME) shouldBe IRIS.COMPONENT_NAME
      log.getString(LoggingKeys.SUBSYSTEM) shouldBe CSW.name
      log.getString(LoggingKeys.PREFIX) shouldBe prefix.toString
      log.getString(LoggingKeys.ACTOR) shouldBe irisActorRef.path.toString
      log.getString(LoggingKeys.FILE) shouldBe IRIS.FILE_NAME
      // todo : create method getNumber as extension to JsObject.
      log(LoggingKeys.LINE).as[Int] shouldBe logMsgLineNumber
      log.getString(LoggingKeys.CLASS).toString shouldBe (IRIS.CLASS_NAME + "#behavior")
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-115: Format and control logging content
  // DEOPSCSW-121: Define structured tags for log messages
  test("message logged with custom Map properties should get logged | DEOPSCSW-115, DEOPSCSW-121") {
    irisActorRef ! LogErrorWithMap("Unknown")
    Thread.sleep(300)

    val errorLevelLogMessages = logBuffer.groupBy(json => json.getString(LoggingKeys.SEVERITY))("ERROR")
    errorLevelLogMessages.size shouldEqual 1

    val expectedMessage  = "Logging error with map"
    val expectedReason   = "Unknown"
    val expectedActorRef = irisActorRef.toString
    errorLevelLogMessages.head.getString("message") shouldBe expectedMessage
    errorLevelLogMessages.head.getString("reason") shouldBe expectedReason
    errorLevelLogMessages.head.getString("actorRef") shouldBe expectedActorRef
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages | DEOPSCSW-126") {

    sendMessagesToActor()
    //  IrisSupervisorActor is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog = logBuffer.groupBy(json => json.getString(LoggingKeys.COMPONENT_NAME))
    val irisLogs                 = groupByComponentNamesLog(IRIS.COMPONENT_NAME)

    irisLogs.size shouldBe 3

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    irisLogs.toList.foreach { log =>
      log.contains(LoggingKeys.ACTOR) shouldBe true
      val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
      Level(currentLogLevel) >= ERROR shouldBe true
    }

  }
}
