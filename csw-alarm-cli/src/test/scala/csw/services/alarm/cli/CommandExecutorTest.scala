package csw.services.alarm.cli

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{LGSF, NFIRAOS, TCS}
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.ExplicitAlarmSeverity.{Major, Okay}
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, Unshelved}
import csw.services.alarm.cli.args.Options
import csw.services.alarm.cli.utils.IterableExtensions.RichStringIterable
import csw.services.config.api.models.ConfigData
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}

class CommandExecutorTest extends AlarmCliTestSetup {

  import cliWiring._

  private val adminService = alarmAdminClient.alarmServiceF.futureValue

  private val successMsg = "[SUCCESS] Command executed successfully."
  private val failureMsg = "[FAILURE] Failed to execute the command."

  private val tromboneAxisLowLimitKey  = AlarmKey(NFIRAOS, "trombone", "tromboneaxislowlimitalarm")
  private val tromboneAxisHighLimitKey = AlarmKey(NFIRAOS, "trombone", "tromboneaxishighlimitalarm")
  private val cpuExceededKey           = AlarmKey(TCS, "tcspk", "cpuexceededalarm")
  private val cpuIdleKey               = AlarmKey(LGSF, "tcspkinactive", "cpuidlealarm")

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

    metadata.map(_.alarmKey).toSet shouldEqual allAlarmKeys
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

    metadata.map(_.alarmKey).toSet shouldEqual allAlarmKeys

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
    val updateCmd = Options(
      "severity",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name),
      severity = Some(Major)
    )
    commandExecutor.execute(updateCmd)

    adminService.getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Major

    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // fetch severity of an alarm
    val getCmd = Options(
      "severity",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )
    commandExecutor.execute(getCmd)

    logBuffer shouldEqual List("Current Alarm Severity: Disconnected")
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should acknowledge the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Unacknowledged

    // acknowledge the alarm
    val ackCmd = Options(
      "acknowledge",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(ackCmd)

    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Acknowledged
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should unacknowledge the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    adminService.acknowledge(tromboneAxisLowLimitKey).futureValue
    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Acknowledged

    // unacknowledge the alarm
    val unackCmd = Options(
      "unacknowledge",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(unackCmd)

    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Unacknowledged
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should activate the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // activate the alarm
    val activateCmd = Options(
      "activate",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(activateCmd)

    adminService.getMetadata(tromboneAxisLowLimitKey).futureValue.activationStatus shouldBe Active
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should deactivate the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // deactivate the alarm
    val deactivateCmd = Options(
      "deactivate",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(deactivateCmd)

    adminService.getMetadata(tromboneAxisLowLimitKey).futureValue.activationStatus shouldBe Inactive
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-473: Shelve/Unshelve alarm from CLI interface
  test("should shelve the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Unshelved

    // shelve the alarm
    val shelveCmd = Options(
      "shelve",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(shelveCmd)

    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Shelved
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-473: Shelve/Unshelve alarm from CLI interface
  test("should unshelve the alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    adminService.shelve(tromboneAxisLowLimitKey).futureValue
    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Shelved

    // unshelve the alarm
    val unshelveCmd = Options(
      "unshelve",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(unshelveCmd)

    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Unshelved
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list all alarms present in the alarm store") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // list alarms
    val listCmd = Options("list")

    commandExecutor.execute(listCmd)

    logBuffer shouldEqualContentsOf "metadata/all_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list alarms for specified subsystem") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // list alarms
    val listCmd = Options("list", maybeSubsystem = Some(NFIRAOS))

    commandExecutor.execute(listCmd)

    logBuffer shouldEqualContentsOf "metadata/subsystem_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list alarms for specified component") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // list alarms
    val listCmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone")
    )

    commandExecutor.execute(listCmd)

    logBuffer shouldEqualContentsOf "metadata/component_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list the alarm for specified name") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    // list alarms
    val listCmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(listCmd)

    logBuffer shouldEqualContentsOf "metadata/with_name_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should fail on invalid component/alarm name") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    val invalidComponentCmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("invalid")
    )

    val invalidAlarmNameCmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some("invalid")
    )

    a[KeyNotFoundException] shouldBe thrownBy(commandExecutor.execute(invalidComponentCmd))
    a[KeyNotFoundException] shouldBe thrownBy(commandExecutor.execute(invalidAlarmNameCmd))
  }

  // DEOPSCSW-474: Latch an alarm from CLI Interface
  test("should reset the severity of latched alarm") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    val resetCmd = Options(
      "reset",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    adminService.setSeverity(tromboneAxisLowLimitKey, Major).futureValue
    adminService.setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Major

    commandExecutor.execute(resetCmd) // reset latch severity of the alarm

    logBuffer shouldEqual List(successMsg)
    adminService.getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Okay
  }

  // DEOPSCSW-475: Fetch alarm status from CLI Interface
  test("should get alarm status") {

    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options("init", Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()

    val statusCmd = Options(
      "status",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(statusCmd)

    logBuffer shouldEqualContentsOf "status.txt"
  }

}
