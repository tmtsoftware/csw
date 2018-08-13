package csw.services.alarm.cli

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.{InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.{Critical, Major}
import csw.services.alarm.api.models.Key.GlobalKey
import csw.services.alarm.cli.args.Options
import csw.services.alarm.cli.helpers.TestFutureExt.RichFuture
import csw.services.config.api.models.ConfigData
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class CommandExecutorTest extends FunSuite with Matchers with SeedData with BeforeAndAfterAll {

  import cliWiring._

  private val adminService = alarmAdminClient.alarmServiceF.await

  private val successMsg = "[SUCCESS] Command executed successfully."
  private val failureMsg = "[FAILURE] Failed to execute the command."

  override def afterAll(): Unit = {
    val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))
    testFileUtils.deleteServerFiles()
    super.afterAll()
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from local config") {
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val args     = Options("init", Some(filePath), isLocal = true)

    commandExecutor.execute(args)
    logBuffer shouldEqual List(successMsg)

    val metadata = adminService.getMetadata(GlobalKey).await

    metadata.map(_.alarmKey.value).toSet shouldEqual Set(
      "nfiraos.trombone.tromboneaxishighlimitalarm",
      "nfiraos.trombone.tromboneaxislowlimitalarm",
      "tcs.tcspk.cpuexceededalarm",
      "lgsf.tcspkinactive.cpuidlealarm"
    )
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from remote config") {
    val serverWiring = ServerWiring.make(locationService)
    serverWiring.svnRepo.initSvnRepo()
    serverWiring.httpService.registeredLazyBinding.await

    val configData    = ConfigData.fromPath(Paths.get(getClass.getResource("/valid-alarms.conf").getPath))
    val configPath    = Paths.get("valid-alarms.conf")
    val configService = ConfigClientFactory.adminApi(system, locationService)
    configService.create(configPath, configData, comment = "commit test file").await

    val args = Options("init", Some(configPath))
    commandExecutor.execute(args)

    logBuffer shouldEqual List(successMsg)

    val metadata = adminService.getMetadata(GlobalKey).await

    metadata.map(_.alarmKey.value).toSet shouldEqual Set(
      "nfiraos.trombone.tromboneaxishighlimitalarm",
      "nfiraos.trombone.tromboneaxislowlimitalarm",
      "tcs.tcspk.cpuexceededalarm",
      "lgsf.tcspkinactive.cpuidlealarm"
    )

    configService.delete(configPath, "deleting test file").await
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API

  test("should fail to initialize alarms in alarm store when config service is down") {
    val configPath = Paths.get("valid-alarms.conf")
    val args       = Options("init", Some(configPath))
    intercept[RuntimeException] {
      commandExecutor.execute(args)
    }
    logBuffer shouldEqual List(failureMsg)
  }

  // DEOPSCSW-480: Set alarm Severity from CLI Interface
  test("should set severity of alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // update severity of an alarm
    val updateCmd = Options(
      "update",
      subsystem = "NFIRAOS",
      component = "trombone",
      name = "tromboneAxisHighLimitAlarm",
      severity = Major
    )
    commandExecutor.execute(updateCmd)

    adminService.getCurrentSeverity(updateCmd.alarmKey).await shouldBe Major

    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should acknowledge the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // update severity of an alarm
    val ackCmd = Options(
      "acknowledge",
      subsystem = "NFIRAOS",
      component = "trombone",
      name = "tromboneAxisLowLimitAlarm"
    )

    adminService.getStatus(ackCmd.alarmKey).await.acknowledgementStatus shouldBe UnAcknowledged

    commandExecutor.execute(ackCmd)

    adminService.getStatus(ackCmd.alarmKey).await.acknowledgementStatus shouldBe Acknowledged

    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should activate the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // update severity of an alarm
    val ackCmd = Options(
      "activate",
      subsystem = "NFIRAOS",
      component = "trombone",
      name = "tromboneAxisLowLimitAlarm"
    )

    commandExecutor.execute(ackCmd)

    adminService.getMetadata(ackCmd.alarmKey).await.activationStatus shouldBe Active
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should deactivate the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // update severity of an alarm
    val ackCmd = Options(
      "deactivate",
      subsystem = "NFIRAOS",
      component = "trombone",
      name = "tromboneAxisLowLimitAlarm"
    )

    commandExecutor.execute(ackCmd)

    adminService.getMetadata(ackCmd.alarmKey).await.activationStatus shouldBe Inactive
    logBuffer shouldEqual List(successMsg)
  }
}
