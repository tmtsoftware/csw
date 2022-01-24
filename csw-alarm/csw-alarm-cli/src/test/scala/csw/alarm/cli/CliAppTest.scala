package csw.alarm.cli

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.cli.args.Options
import csw.alarm.cli.utils.IterableExtensions.RichStringIterable
import csw.alarm.cli.utils.TestFutureExt.RichFuture
import csw.alarm.commons.Separators.KeySeparator
import csw.alarm.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.models.ActivationStatus.{Active, Inactive}
import csw.alarm.models.AlarmHealth
import csw.alarm.models.AlarmHealth.{Bad, Good, Ill}
import csw.alarm.models.AlarmSeverity._
import csw.alarm.models.AutoRefreshSeverityMessage.CancelAutoRefresh
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key.{AlarmKey, GlobalKey}
import csw.alarm.models.ShelveStatus.{Shelved, Unshelved}
import csw.commons.ResourceReader
import csw.config.api.ConfigData
import csw.config.client.scaladsl.ConfigClientFactory
import csw.config.server.commons.TestFileUtils
import csw.config.server.mocks.MockedAuthentication
import csw.config.server.{ServerWiring, Settings}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{LGSF, NFIRAOS, TCS}

//CSW-83:Alarm models should take prefix
class CliAppTest extends AlarmCliTestSetup with MockedAuthentication {

  import cliWiring._
  import commandLineRunner.alarmService._

  private val successMsg = "[SUCCESS] Command executed successfully."
  private val failureMsg = "[FAILURE] Failed to execute the command."

  private val tromboneAxisLowLimitKey  = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneaxislowlimitalarm")
  private val tromboneAxisHighLimitKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneaxishighlimitalarm")
  private val encTempLowKey            = AlarmKey(Prefix(NFIRAOS, "enclosure"), "templowalarm")
  private val encTempHighKey           = AlarmKey(Prefix(NFIRAOS, "enclosure"), "temphighalarm")
  private val beamSplitterLimitKey     = AlarmKey(Prefix(NFIRAOS, "beamsplitter"), "splitterlimitalarm")
  private val cpuExceededKey           = AlarmKey(Prefix(TCS, "tcspk"), "cpuexceededalarm")
  private val outOfRangeOffloadKey     = AlarmKey(Prefix(TCS, "corrections"), "outofrangeoffload")
  private val cpuIdleKey               = AlarmKey(Prefix(LGSF, "tcspkinactive"), "cpuidlealarm")

  private val allAlarmKeys = Set(
    tromboneAxisLowLimitKey,
    tromboneAxisHighLimitKey,
    encTempLowKey,
    encTempHighKey,
    beamSplitterLimitKey,
    cpuExceededKey,
    outOfRangeOffloadKey,
    cpuIdleKey
  )

  override def beforeEach(): Unit = {
    // init alarm store
    val filePath = ResourceReader.copyToTmp("/valid-alarms.conf")
    val initCmd  = Options(cmd = "init", filePath = Some(filePath), isLocal = true, reset = true)
    cliApp.execute(initCmd)
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))
    testFileUtils.deleteServerFiles()
    super.afterAll()
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from local config | DEOPSCSW-470") {
    val filePath = ResourceReader.copyToTmp("/valid-alarms.conf")
    val cmd      = Options(cmd = "init", filePath = Some(filePath), isLocal = true, reset = true)

    clearAlarmStore().futureValue
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)

    cliApp.execute(cmd) // initialize alarm store
    logBuffer.toList shouldEqual List(successMsg)

    val metadata = getMetadata(GlobalKey).futureValue

    metadata.map(_.alarmKey).toSet shouldEqual allAlarmKeys
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from remote config | DEOPSCSW-470") {
    val serverWiring = ServerWiring.make(locationService, securityDirectives)
    serverWiring.svnRepo.initSvnRepo()
    val (binding, regResult) = serverWiring.httpService.registeredLazyBinding.futureValue

    val configData    = ConfigData.fromPath(ResourceReader.copyToTmp("/valid-alarms.conf"))
    val configPath    = Paths.get("valid-alarms.conf")
    val configService = ConfigClientFactory.adminApi(actorRuntime.actorSystem, locationService, factory)
    configService.create(configPath, configData, comment = "commit test file").futureValue

    val cmd = Options(cmd = "init", filePath = Some(configPath), reset = true)

    clearAlarmStore().futureValue
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)

    cliApp.execute(cmd) // initialize alarm store
    logBuffer.toList shouldEqual List(successMsg)
    val metadata = getMetadata(GlobalKey).futureValue
    metadata.map(_.alarmKey).toSet shouldEqual allAlarmKeys

    // clean up
    configService.delete(configPath, "deleting test file").futureValue
    regResult.unregister().futureValue
    binding.unbind().futureValue
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should fail to initialize alarms in alarm store when config service is down | DEOPSCSW-470") {
    val configPath = Paths.get("valid-alarms.conf")
    val cmd        = Options(cmd = "init", filePath = Some(configPath), reset = true)

    clearAlarmStore().futureValue
    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(failureMsg)
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should acknowledge the alarm | DEOPSCSW-471") {
    val cmd = Options(
      "acknowledge",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    unacknowledge(tromboneAxisLowLimitKey).futureValue
    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Unacknowledged

    cliApp.execute(cmd) // acknowledge the alarm

    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Acknowledged
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-471: Acknowledge alarm from CLI application
  test("should unacknowledge the alarm | DEOPSCSW-471") {
    val cmd = Options(
      "unacknowledge",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Acknowledged
    cliApp.execute(cmd) // unacknowledge the alarm
    getStatus(tromboneAxisLowLimitKey).futureValue.acknowledgementStatus shouldBe Unacknowledged
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should activate the alarm | DEOPSCSW-472") {
    val cmd = Options(
      "activate",
      maybeSubsystem = Some(cpuIdleKey.prefix.subsystem),
      maybeComponent = Some(cpuIdleKey.prefix.componentName),
      maybeAlarmName = Some(cpuIdleKey.name)
    )

    getMetadata(cpuIdleKey).futureValue.activationStatus shouldBe Inactive
    cliApp.execute(cmd) // activate the alarm
    getMetadata(cpuIdleKey).futureValue.activationStatus shouldBe Active
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-472: Exercise Alarm CLI for activate/out of service alarm behaviour
  test("should deactivate the alarm | DEOPSCSW-472") {
    val cmd = Options(
      "deactivate",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    getMetadata(tromboneAxisLowLimitKey).futureValue.activationStatus shouldBe Active
    cliApp.execute(cmd) // deactivate the alarm
    getMetadata(tromboneAxisLowLimitKey).futureValue.activationStatus shouldBe Inactive
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-473: Shelve/Unshelve alarm from CLI interface
  test("should shelve the alarm | DEOPSCSW-473") {
    val cmd = Options(
      "shelve",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Unshelved
    cliApp.execute(cmd) // shelve the alarm
    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Shelved
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-473: Shelve/Unshelve alarm from CLI interface
  test("should unshelve the alarm | DEOPSCSW-473") {
    val cmd = Options(
      "unshelve",
      maybeSubsystem = Some(tromboneAxisLowLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisLowLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    shelve(tromboneAxisLowLimitKey).futureValue
    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Shelved

    cliApp.execute(cmd) // unshelve the alarm

    getStatus(tromboneAxisLowLimitKey).futureValue.shelveStatus shouldBe Unshelved
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  test("should list all alarms present in the alarm store | DEOPSCSW-492, DEOPSCSW-503") {
    val cmd = Options("list")

    cliApp.execute(cmd)
    logBuffer.toList shouldEqualContentsOf "list/all_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  test("should list alarms for specified subsystem | DEOPSCSW-492, DEOPSCSW-503") {
    val cmd = Options("list", maybeSubsystem = Some(NFIRAOS))

    cliApp.execute(cmd)
    logBuffer.toList shouldEqualContentsOf "list/subsystem_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  test("should list alarms for specified component | DEOPSCSW-492, DEOPSCSW-503") {
    val cmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone")
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqualContentsOf "list/component_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  test("should list the alarm for specified name | DEOPSCSW-492, DEOPSCSW-503") {
    val cmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqualContentsOf "list/with_name_alarms.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  test("should list the metadata of alarm for specified name | DEOPSCSW-492, DEOPSCSW-503") {
    val cmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name),
      showStatus = false
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqualContentsOf "metadata.txt"
  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  // DEOPSCSW-475: Fetch alarm status from CLI Interface
  test("should list status of alarms | DEOPSCSW-492, DEOPSCSW-503, DEOPSCSW-475") {
    val cmd = Options(
      "list",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name),
      showMetadata = false
    )

    cliApp.execute(cmd)
    // alarm time changes on every run hence filter out time before assertion
    logBuffer.toList shouldEqualContentsOf "status.txt"

  }

  // DEOPSCSW-492: Fetch all alarms' metadata from CLI Interface (list all alarms)
  // DEOPSCSW-503: List alarms should show all data of an alarm (metadata, status, severity)
  test("should list no alarm for invalid key/pattern | DEOPSCSW-492, DEOPSCSW-503") {
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

    cliApp.execute(invalidComponentCmd)
    cliApp.execute(invalidAlarmNameCmd)
    logBuffer.toList shouldEqual Array.fill(2)("No matching keys found.")
  }

  // DEOPSCSW-474: Latch an alarm from CLI Interface
  test("should reset the severity of latched alarm | DEOPSCSW-474") {
    val cmd = Options(
      "reset",
      maybeSubsystem = Some(NFIRAOS),
      maybeComponent = Some("trombone"),
      maybeAlarmName = Some(tromboneAxisLowLimitKey.name)
    )

    getCurrentSeverity(tromboneAxisLowLimitKey).futureValue shouldBe Disconnected
    getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Disconnected

    setSeverity(tromboneAxisLowLimitKey, Major).futureValue
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Major

    cliApp.execute(cmd) // reset latch severity of the alarm

    logBuffer.toList shouldEqual List(successMsg)
    getStatus(tromboneAxisLowLimitKey).futureValue.latchedSeverity shouldBe Okay
  }

  // -------------------------------------------Severity--------------------------------------------

  // DEOPSCSW-480: Set alarm Severity from CLI Interface
  test("should set severity of alarm | DEOPSCSW-480") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "set",
      severity = Some(Major),
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Disconnected
    cliApp.execute(cmd) // update severity of an alarm
    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Major
    logBuffer.toList shouldEqual List(successMsg)
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of alarm | DEOPSCSW-476") {
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue

    val cmd = Options(
      cmd = "severity",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(s"Severity of Alarm [${cmd.alarmKey}]: $Okay")
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of a component | DEOPSCSW-476") {
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    setSeverity(tromboneAxisHighLimitKey, Major).futureValue

    val cmd = Options(
      cmd = "severity",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(
      s"Aggregated Severity of Component [${tromboneAxisHighLimitKey.prefix.subsystem.name}$KeySeparator${tromboneAxisHighLimitKey.prefix.componentName}]: $Major"
    )
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of a subsystem | DEOPSCSW-476") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(s"Aggregated Severity of Subsystem [${cmd.maybeSubsystem.get}]: $Disconnected")
  }

  // DEOPSCSW-476: Fetch alarm severity from CLI Interface
  test("should get severity of Alarm Service | DEOPSCSW-476") {
    setSeverity(cpuExceededKey, Indeterminate).futureValue

    val cmd = Options(cmd = "severity", subCmd = "get")

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(s"Aggregated Severity of Alarm Service: $Disconnected")
  }

  // DEOPSCSW-477: Subscribe to alarm severities for component or subsystem from CLI Interface
  test("should subscribe severity of alarm | DEOPSCSW-477") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "subscribe",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    val (subscription, _) = commandLineRunner.subscribeSeverity(cmd)
    subscription.ready().futureValue

    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Disconnected

    setSeverity(tromboneAxisHighLimitKey, Major).futureValue
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue

    eventually(
      logBuffer.toList shouldEqual List(
        s"Severity of Alarm [$tromboneAxisHighLimitKey]: $Disconnected",
        s"Severity of Alarm [$tromboneAxisHighLimitKey]: $Major",
        s"Severity of Alarm [$tromboneAxisHighLimitKey]: $Okay"
      )
    )

    subscription.unsubscribe().futureValue
  }

  // DEOPSCSW-491: Auto-refresh an alarm through alarm service cli
  // DEOPSCSW-507: Auto-refresh utility for component developers
  test("should refresh severity of alarm | DEOPSCSW-491, DEOPSCSW-507") {
    val cmd = Options(
      cmd = "severity",
      subCmd = "set",
      severity = Some(Major),
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name),
      autoRefresh = true
    )

    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Disconnected
    val actorRef = commandLineRunner.refreshSeverity(cmd)
    Thread.sleep(500)
    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Major
    Thread.sleep(1200) // Waiting for severity to timeout to Disconnected
    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Major

    val expectedMsg = s"Severity for [$tromboneAxisHighLimitKey] refreshed to: $Major"
    logBuffer.toList shouldEqual List(expectedMsg, expectedMsg)
    actorRef ! CancelAutoRefresh(tromboneAxisHighLimitKey)
  }

  // -------------------------------------------Health--------------------------------------------

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of alarm | DEOPSCSW-478") {
    val cmd = Options(
      cmd = "health",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName),
      maybeAlarmName = Some(tromboneAxisHighLimitKey.name)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(s"Health of Alarm [${cmd.alarmKey}]: $Bad")
  }

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of component | DEOPSCSW-478") {
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue
    setSeverity(tromboneAxisLowLimitKey, Major).futureValue

    val cmd = Options(
      cmd = "health",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem),
      maybeComponent = Some(tromboneAxisHighLimitKey.prefix.componentName)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(
      s"Aggregated Health of Component [${tromboneAxisHighLimitKey.prefix.subsystem.name}$KeySeparator${tromboneAxisHighLimitKey.prefix.componentName}]: $Ill"
    )
  }

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of subsystem | DEOPSCSW-478") {
    setSeverity(tromboneAxisHighLimitKey, Warning).futureValue
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    setSeverity(outOfRangeOffloadKey, Okay).futureValue
    setSeverity(beamSplitterLimitKey, Okay).futureValue
    setSeverity(encTempHighKey, Okay).futureValue
    setSeverity(encTempLowKey, Okay).futureValue

    val cmd = Options(
      cmd = "health",
      subCmd = "get",
      maybeSubsystem = Some(tromboneAxisHighLimitKey.prefix.subsystem)
    )

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(s"Aggregated Health of Subsystem [${cmd.maybeSubsystem.get}]: $Good")
  }

  // DEOPSCSW-478: Fetch health of component/subsystem from CLI Interface
  test("should get health of alarm service | DEOPSCSW-478") {
    val cmd = Options(cmd = "health", subCmd = "get")

    cliApp.execute(cmd)
    logBuffer.toList shouldEqual List(s"Aggregated Health of Alarm Service: $Bad")
  }

  // DEOPSCSW-479: Subscribe to health changes of component/subsystem/all alarms using CLI Interface
  test("should subscribe health of subsystem/component/alarm | DEOPSCSW-479") {
    val subsystem         = tromboneAxisHighLimitKey.prefix.subsystem
    val component         = tromboneAxisHighLimitKey.prefix.componentName
    val tromboneComponent = s"${subsystem.name}-$component"

    val cmd = Options(
      cmd = "health",
      subCmd = "subscribe",
      maybeSubsystem = Some(subsystem),
      maybeComponent = Some(component)
    )

    val (subscription, _) = commandLineRunner.subscribeHealth(cmd)
    subscription.ready().futureValue

    getCurrentSeverity(tromboneAxisHighLimitKey).futureValue shouldBe Disconnected
    getCurrentSeverity(tromboneAxisLowLimitKey).futureValue shouldBe Disconnected

    // initially aggregated Health of Component [NFIRAOS-trombone] will be Bad
    assertThatAggregatedHealthOfComponentIs(tromboneComponent, Bad)

    // Verify that after setting severity of all component alarms to major, health status becomes Ill
    setSeverity(tromboneAxisHighLimitKey, Major).futureValue
    setSeverity(tromboneAxisLowLimitKey, Major).futureValue
    assertThatAggregatedHealthOfComponentIs(tromboneComponent, Ill)

    // Verify that after setting severity of all component alarms to Okay, health status becomes Good
    setSeverity(tromboneAxisHighLimitKey, Okay).futureValue
    setSeverity(tromboneAxisLowLimitKey, Okay).futureValue
    assertThatAggregatedHealthOfComponentIs(tromboneComponent, Good)

    subscription.unsubscribe().futureValue
  }

  def assertThatAggregatedHealthOfComponentIs(compName: String, health: AlarmHealth): Unit = {
    eventually(logBuffer.toList shouldEqual List(s"Aggregated Health of Component [$compName]: $health"))
    logBuffer.clear()
  }

}
