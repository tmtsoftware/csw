package csw.services.alarm.cli

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.Major
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.cli.args.Options
import csw.services.config.api.models.ConfigData
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}

class CommandExecutorTest extends AlarmCliTestSetup {

  import cliWiring._

  private val adminService = alarmAdminClient.alarmServiceF.futureValue

  private val successMsg = "[SUCCESS] Command executed successfully."
  private val failureMsg = "[FAILURE] Failed to execute the command."

  private val tromboneAxisLowLimitKey  = AlarmKey("nfiraos.trombone.tromboneaxislowlimitalarm")
  private val tromboneAxisHighLimitKey = AlarmKey("nfiraos.trombone.tromboneaxishighlimitalarm")
  private val cpuExceededKey           = AlarmKey("tcs.tcspk.cpuexceededalarm")
  private val cpuIdleKey               = AlarmKey("lgsf.tcspkinactive.cpuidlealarm")

  private val allAlarmKeys = Set(tromboneAxisLowLimitKey, tromboneAxisHighLimitKey, cpuExceededKey, cpuIdleKey)

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

    val metadata = adminService.getMetadata(GlobalKey).futureValue

    metadata.map(_.alarmKey.value).toSet shouldEqual allAlarmKeys.map(_.value)
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from remote config") {
    val serverWiring = ServerWiring.make(locationService)
    serverWiring.svnRepo.initSvnRepo()
    val (binding, regResult) = serverWiring.httpService.registeredLazyBinding.futureValue

    val configData    = ConfigData.fromPath(Paths.get(getClass.getResource("/valid-alarms.conf").getPath))
    val configPath    = Paths.get("valid-alarms.conf")
    val configService = ConfigClientFactory.adminApi(system, locationService)
    configService.create(configPath, configData, comment = "commit test file").futureValue

    val args = Options("init", Some(configPath))
    commandExecutor.execute(args)

    logBuffer shouldEqual List(successMsg)

    val metadata = adminService.getMetadata(GlobalKey).futureValue

    metadata.map(_.alarmKey.value).toSet shouldEqual allAlarmKeys.map(_.value)

    // clean up
    configService.delete(configPath, "deleting test file").futureValue
    regResult.unregister().futureValue
    binding.unbind().futureValue
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should fail to initialize alarms in alarm store when config service is down") {
    val configPath = Paths.get("valid-alarms.conf")
    val args       = Options("init", Some(configPath))
    an[RuntimeException] shouldBe thrownBy(commandExecutor.execute(args))
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
    val updateCmd = Options("update", alarmKey = tromboneAxisHighLimitKey, severity = Major)
    commandExecutor.execute(updateCmd)

    adminService.getCurrentSeverity(updateCmd.alarmKey).futureValue shouldBe Major

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
    val ackCmd = Options("acknowledge", alarmKey = tromboneAxisLowLimitKey)

    adminService.getStatus(ackCmd.alarmKey).futureValue.acknowledgementStatus shouldBe UnAcknowledged

    commandExecutor.execute(ackCmd)

    adminService.getStatus(ackCmd.alarmKey).futureValue.acknowledgementStatus shouldBe Acknowledged

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
    val ackCmd = Options("activate", alarmKey = tromboneAxisLowLimitKey)

    commandExecutor.execute(ackCmd)

    adminService.getMetadata(ackCmd.alarmKey).futureValue.activationStatus shouldBe Active
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
    val ackCmd = Options("deactivate", alarmKey = tromboneAxisLowLimitKey)

    commandExecutor.execute(ackCmd)

    adminService.getMetadata(ackCmd.alarmKey).futureValue.activationStatus shouldBe Inactive
    logBuffer shouldEqual List(successMsg)
  }
}
