/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.appenders

import java.nio.file.Paths

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.SpawnProtocol
import com.typesafe.config.ConfigFactory
import csw.logging.client.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.exceptions.BaseLogPathNotDefined
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.utils.FileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-123: Allow local component logs to be output to a file
// DEOPSCSW-649: Fixed directory configuration for multi JVM scenario
class FileAppenderTest extends AnyFunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  private val logFileDir                = Paths.get("/tmp/csw-test-logs/").toFile
  private val actorSystem               = ActorSystem(SpawnProtocol(), "test-1")
  private val standardHeaders: JsObject = Json.obj(LoggingKeys.HOST -> "localhost", LoggingKeys.NAME -> "test-service")

  private val fileAppender = new FileAppender(actorSystem, standardHeaders)

  val logMsgString1: String =
    s"""{
      |  "${LoggingKeys.CATEGORY}": "alternative",
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.SUBSYSTEM}": "csw",
      |  "${LoggingKeys.PREFIX}": "csw.FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.NAME}": "test-service",
      |  "${LoggingKeys.SEVERITY}": "ERROR",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-19T16:10:19.397Z",
      |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString2: String =
    s"""{
      |  "${LoggingKeys.CATEGORY}": "${Category.Common.name}",
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.SUBSYSTEM}": "csw",
      |  "${LoggingKeys.PREFIX}": "csw.FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.NAME}": "test-service",
      |  "${LoggingKeys.SEVERITY}": "ERROR",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-20T16:10:19.397Z",
      |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString3: String =
    s"""{
      |  "${LoggingKeys.CATEGORY}": "${Category.Common.name}",
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.SUBSYSTEM}": "csw",
      |  "${LoggingKeys.PREFIX}": "csw.FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.NAME}": "test-service",
      |  "${LoggingKeys.SEVERITY}": "INFO",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-23T01:10:19.397Z",
      |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at INFO level"
      |}
    """.stripMargin

  val expectedLogMsgJson1: JsObject = Json.parse(logMsgString1).as[JsObject]
  val expectedLogMsgJson2: JsObject = Json.parse(logMsgString2).as[JsObject]
  val expectedLogMsgJson3: JsObject = Json.parse(logMsgString3).as[JsObject]

  private val date1            = expectedLogMsgJson1.getString(LoggingKeys.TIMESTAMP)
  private val localDateTime1   = FileAppender.decideTimestampForFile(TMTDateTimeFormatter.parse(date1))
  private val logFileFullPath1 = logFileDir.getAbsolutePath ++ s"/test-service_${localDateTime1}_alternative.log"

  private val date2            = expectedLogMsgJson2.getString(LoggingKeys.TIMESTAMP)
  private val localDateTime2   = FileAppender.decideTimestampForFile(TMTDateTimeFormatter.parse(date2))
  private val logFileFullPath2 = logFileDir.getAbsolutePath ++ s"/test-service_$localDateTime2.log"

  private val date3            = expectedLogMsgJson3.getString(LoggingKeys.TIMESTAMP)
  private val localDateTime3   = FileAppender.decideTimestampForFile(TMTDateTimeFormatter.parse(date3))
  private val logFileFullPath3 = logFileDir.getAbsolutePath ++ s"/test-service_$localDateTime3.log"

  override protected def beforeAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }

  // DEOPSCSW-649: Fixed directory configuration for multi JVM scenario
  test("should throw BaseLogPathNotDefined exception if TMT_LOG_HOME is not defined | DEOPSCSW-123, DEOPSCSW-649") {
    val config = ConfigFactory.parseString("""
                                             |include "logging.conf"
                                             |csw-logging {
                                             | appenders = ["csw.logging.client.appenders.FileAppender$"]
                                             | appender-config {
                                             |  file {
                                             |      baseLogPath = ${?TMT_LOG_HOME}
                                             |  }
                                             | }
                                             |}
                                           """.stripMargin)

    intercept[BaseLogPathNotDefined] {
      new FileAppender(ActorSystem(SpawnProtocol(), "test-2", config.resolve()), standardHeaders)
    }
  }

  // DEOPSCSW-151 : Manage log file size
  // CSW-78: PrefixRedesign for logging
  test("log file is rotated every day | DEOPSCSW-123, DEOPSCSW-649, DEOPSCSW-151") {
    fileAppender.append(expectedLogMsgJson1, "alternative")
    fileAppender.append(expectedLogMsgJson2, Category.Common.name)
    fileAppender.append(expectedLogMsgJson3, Category.Common.name)
    Thread.sleep(10)

    val actualLogBuffer1 = FileUtils.read(logFileFullPath1).toList
    val actualLogBuffer2 = FileUtils.read(logFileFullPath2).toList
    val actualLogBuffer3 = FileUtils.read(logFileFullPath3).toList

    actualLogBuffer1.head shouldBe expectedLogMsgJson1
    actualLogBuffer2.head shouldBe expectedLogMsgJson2
    actualLogBuffer3.head shouldBe expectedLogMsgJson3
  }
}
