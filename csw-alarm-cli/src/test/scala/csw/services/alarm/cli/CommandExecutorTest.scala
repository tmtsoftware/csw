package csw.services.alarm.cli

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{LGSF, NFIRAOS, TCS}
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmHealth.{Bad, Good, Ill}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.AlarmStatus
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
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
  import alarmAdminClient.alarmService._

  private val successMsg = "[SUCCESS] Command executed successfully."
  private val failureMsg = "[FAILURE] Failed to execute the command."

  private val tromboneAxisLowLimitKey  = AlarmKey(NFIRAOS, "trombone", "tromboneaxislowlimitalarm")
  private val tromboneAxisHighLimitKey = AlarmKey(NFIRAOS, "trombone", "tromboneaxishighlimitalarm")
  private val cpuExceededKey           = AlarmKey(TCS, "tcspk", "cpuexceededalarm")
  private val cpuIdleKey               = AlarmKey(LGSF, "tcspkinactive", "cpuidlealarm")

  private val allAlarmKeys = Set(tromboneAxisLowLimitKey, tromboneAxisHighLimitKey, cpuExceededKey, cpuIdleKey)

  override def beforeEach(): Unit = {
    // init alarm store
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val initCmd  = Options(cmd = "init", filePath = Some(filePath), isLocal = true, reset = true)
    commandExecutor.execute(initCmd)
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))
    testFileUtils.deleteServerFiles()
    super.afterAll()
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from local config") {
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val args     = Options(cmd = "init", filePath = Some(filePath), isLocal = true, reset = true)

    commandExecutor.execute(args)
    logBuffer shouldEqual List(successMsg)

    val metadata = getMetadata(GlobalKey).futureValue

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

    val args = Options(cmd = "init", filePath = Some(configPath), reset = true)
    commandExecutor.execute(args)

    logBuffer shouldEqual List(successMsg)

    val metadata = getMetadata(GlobalKey).futureValue

    metadata.map(_.alarmKey).toSet shouldEqual allAlarmKeys

    // clean up
    configService.delete(configPath, "deleting test file").futureValue
    regResult.unregister().futureValue
    binding.unbind().futureValue
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should fail to initialize alarms in alarm store when config service is down") {
    val configPath = Paths.get("valid-alarms.conf")
    val args       = Options(cmd = "init", filePath = Some(configPath), reset = true)
    an[RuntimeException] shouldBe thrownBy(commandExecutor.execute(args))
    logBuffer shouldEqual List(failureMsg)
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should acknowledge the alarm") {
    val cmd = Options(
      "acknowledge",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    setStatus(tromboneAxisLowLimitKey, AlarmStatus().copy(acknowledgementStatus = Unacknowledged, latchedSeverity = Critical)).futureValue

    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Unacknowledged
    commandExecutor.execute(cmd) // acknowledge the alarm
    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Acknowledged
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should unacknowledge the alarm") {
    val cmd = Options(
      "unacknowledge",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    acknowledge(tromboneAxisLowLimitKey).futureValue
    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Acknowledged

    commandExecutor.execute(cmd) // unacknowledge the alarm

    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Unacknowledged
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should activate the alarm") {
    val cmd = Options(
      "activate",
      maybeSubsystem = Some(cpuIdleKey.subsystem),
      maybeComponent = Some(cpuIdleKey.component),
      maybeAlarmName = Some(cpuIdleKey.name)
    )

    getMetadata(cpuIdleKey).futureValue.activationStatus shouldBe Inactive

    commandExecutor.execute(cmd) // activate the alarm

    getMetadata(cpuIdleKey).futureValue.activationStatus shouldBe Active
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should deactivate the alarm") {
    val cmd = Options(
      "deactivate",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )
    getMetadata(tromboneAxisLowLimitKey).futureValue.activationStatus shouldBe Active
    commandExecutor.execute(cmd) // deactivate the alarm

    getMetadata(tromboneAxisLowLimitKey).futureValue.activationStatus shouldBe Inactive
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-473: Shelve/Unshelve alarm from CLI interface
  test("should shelve the alarm") {
    val cmd = Options(
      "shelve",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Unshelved
    commandExecutor.execute(cmd) // shelve the alarm
    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Shelved
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-473: Shelve/Unshelve alarm from CLI interface
  test("should unshelve the alarm") {
    val cmd = Options(
      "unshelve",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.component),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    shelve(tromboneAxisLowLimitKey).futureValue
    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Shelved

    commandExecutor.execute(cmd) // unshelve the alarm

    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Unshelved
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list all alarms present in the alarm store") {
    val cmd = Options("list")

    commandExecutor.execute(cmd)
    logBuffer shouldEqualContentsOf "metadata/all_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list alarms for specified subsystem") {
    val cmd = Options("list", maybeSubsystem = Some(NFIRAOS))

    commandExecutor.execute(cmd)
    logBuffer shouldEqualContentsOf "metadata/subsystem_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list alarms for specified component") {
    val cmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone")
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqualContentsOf "metadata/component_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should list the alarm for specified name") {
    val cmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqualContentsOf "metadata/with_name_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  test("should fail on invalid component/alarm name") {
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

    intercept[KeyNotFoundException] { commandExecutor.execute(invalidComponentCmd) }
    intercept[KeyNotFoundException] { commandExecutor.execute(invalidAlarmNameCmd) }
  }

  // DEOPSCSW-474: Latch an alarm from CLI Interface
  test("should reset the severity of latched alarm") {
    val cmd = Options(
      "reset",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    setSeverity(tromboneAxisLowLimitKey, Major).futureValue
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Major

    commandExecutor.execute(cmd) // reset latch severity of the alarm

    logBuffer shouldEqual List(successMsg)
    getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Okay
  }

  // DEOPSCSW-475: Fetch alarm status from CLI Interface
  test("should get alarm status") {
    val cmd = Options(
      "status",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqualContentsOf "status.txt"
  }

  // -------------------------------------------Severity--------------------------------------------

  // DEOPSCSW-480: Set alarm Severity from CLI Interface
  test("should set severity of alarm") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "set",
      severity = Some(Major),
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Disconnected
    commandExecutor.execute(cmd) // update severity of an alarm
    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Major
    logBuffer shouldEqual List(successMsg)
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of alarm") {
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue

    val cmd = Options(
      cmd = "severity",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(s"Severity of Alarm ${cmd.alarmKey.value}: ${Okay.toString}")
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of a component") {
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    setSeverity(tromboneAxisHighLimitKey, Major).futureValue

    val cmd = Options(
      cmd = "severity",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(
      s"Aggregated Severity of Component ${tromboneAxisHighLimitKey.subsystem}.${tromboneAxisHighLimitKey.component}: ${Major.toString}"
    )
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of a subsystem") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(s"Aggregated Severity of Subsystem ${cmd.maybeSubsystem.get}: ${Disconnected.toString}")
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of Alarm Service") {
    setSeverity(cpuExceededKey, Indeterminate).futureValue

    val cmd = Options(cmd = "severity", subCmd = "get")

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(s"Aggregated Severity of Alarm Service: ${Disconnected.toString}")
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("should subscribe severity of alarm") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "subscribe",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    val (subscription, _) = alarmAdminClient.subscribeSeverity(cmd)
    Thread.sleep(1000) //wait for subscription to finish

    setSeverity(tromboneAxisHighLimitKey, Major).futureValue
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue

    logBuffer shouldEqual List(
      s"Severity of Alarm ${tromboneAxisHighLimitKey.value}: ${Major.toString}",
      s"Severity of Alarm ${tromboneAxisHighLimitKey.value}: ${Okay.toString}"
    )

    subscription.unsubscribe().futureValue
  }

  // -------------------------------------------Health--------------------------------------------

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of alarm") {
    val cmd = Options(
      cmd = "health",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(s"Health of Alarm ${cmd.alarmKey.value}: ${Bad.toString}")
  }

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of component") {
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue
    setSeverity(tromboneAxisLowLimitKey, Major).futureValue

    val cmd = Options(
      cmd = "health",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(
      s"Aggregated Health of Component ${tromboneAxisHighLimitKey.subsystem}.${tromboneAxisHighLimitKey.component}: ${Ill.toString}"
    )
  }

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of subsystem") {
    setSeverity(tromboneAxisHighLimitKey, Warning).futureValue
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue

    val cmd = Options(
      cmd = "health",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem)
    )

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(s"Aggregated Health of Subsystem ${cmd.maybeSubsystem.get}: ${Good.toString}")
  }

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of alarm service") {
    val cmd = Options(cmd = "health", subCmd = "get")

    commandExecutor.execute(cmd)
    logBuffer shouldEqual List(s"Aggregated Health of Alarm Service: ${Bad.toString}")
  }

  // DEOPSCSW-479: Subscribe to health changes of component/subsystem/all alarms using CLI Interface
  test("should subscribe health of subsystem/component/alarm") {
    val cmd = Options(
      cmd = "health",
      subCmd = "subscribe",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.component),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    val (subscription, _) = alarmAdminClient.subscribeHealth(cmd)
    Thread.sleep(1000) //wait for subscription to finish

    setSeverity(tromboneAxisHighLimitKey, Major).futureValue
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue

    logBuffer shouldEqual List(
      s"Health of Alarm ${tromboneAxisHighLimitKey.value}: ${Ill.toString}",
      s"Health of Alarm ${tromboneAxisHighLimitKey.value}: ${Good.toString}"
    )

    subscription.unsubscribe().futureValue
  }
}
