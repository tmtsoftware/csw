package csw.services.alarm.client.internal

import java.io.File

import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import csw.services.alarm.api.models.AcknowledgementStatus._
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.helpers.{AcknowledgeAndLatchTestCase, AlarmServiceTestSetupNGTest, SetSeverityTestCase}
import org.testng.annotations.{BeforeSuite, DataProvider, Test}

//DEOPSCSW-444 : Set severity api for component
class SetSeverityTests extends AlarmServiceTestSetupNGTest {

  val `low-latchable->high-latchable` = AlarmKey("test-subsystem", "test-component", "low-latchable->high-latchable")
  val `low-unlatable->hig-latchable`  = AlarmKey("test-subsystem", "test-component", "low-unlatable->hig-latchable")
  val `high-latchable->low-latchable` = AlarmKey("test-subsystem", "test-component", "high-latchable->low-latchable")
  val `high-unlatable->low-latchable` = AlarmKey("test-subsystem", "test-component", "high-unlatable->low-latchable")

  val alarmServiceImpl: AlarmServiceImpl = alarmService.asInstanceOf[AlarmServiceImpl]

  @BeforeSuite
  def seedData(): Unit = {

    val validAlarmsFile   = new File(getClass.getResource("/test-alarms/latchSeverityTestAlarms.conf").getPath)
    val validAlarmsConfig = ConfigFactory.parseFile(validAlarmsFile).resolve(ConfigResolveOptions.noSystem())
    alarmService.initAlarms(validAlarmsConfig, reset = true).await

    List(
      (`low-latchable->high-latchable`, Warning),
      (`low-unlatable->hig-latchable`, Disconnected),
      (`high-latchable->low-latchable`, Major),
      (`high-unlatable->low-latchable`, Disconnected)
    ).foreach {
      case (alarmKey, severity) if severity == Disconnected =>
        alarmServiceImpl.setStatus(alarmKey, AlarmStatus()).await
      case (alarmKey, severity) =>
        alarmServiceImpl
          .setStatus(
            alarmKey,
            AlarmStatus(
              latchedSeverity = severity,
              latchStatus = if (severity.latchable) Latched else UnLatched
            )
          )
          .await
    }
  }

  @DataProvider(name = "setSeverityTest-data-provider")
  def setSeverityTestDataProvider(): Array[SetSeverityTestCase] =
    Array(
      SetSeverityTestCase(
        oldSeverity = Warning,
        newSeverity = Major,
        outcome = Major,
        alarmKey = `low-latchable->high-latchable`
      ),
      SetSeverityTestCase(
        oldSeverity = Disconnected,
        newSeverity = Critical,
        outcome = Critical,
        alarmKey = `low-unlatable->hig-latchable`
      ),
      SetSeverityTestCase(
        oldSeverity = Major,
        newSeverity = Warning,
        outcome = Major,
        alarmKey = `high-latchable->low-latchable`
      ),
      SetSeverityTestCase(
        oldSeverity = Disconnected,
        newSeverity = Major,
        outcome = Major,
        alarmKey = `high-unlatable->low-latchable`
      )
    )

  val ackAndLatchTestCases = Array(
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "AutoAcknowledgeble-Latchable-Disconnected->Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "NotAutoAcknowledgeble-Latchable-Disconnected->Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "AutoAcknowledgeble-NotLatchable-Disconnected->Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "NotAutoAcknowledgeble-NotLatchable-Disconnected->Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "AutoAcknowledgeble-Latchable-Okay->Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "NotAutoAcknowledgeble-Latchable-Okay->Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "AutoAcknowledgeble-NotLatchable-Okay->Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "NotAutoAcknowledgeble-NotLatchable-Okay->Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "AutoAcknowledgeble-Latchable-Critical->Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "NotAutoAcknowledgeble-Latchable-Critical->Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "AutoAcknowledgeble-NotLatchable-Critical->Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("test", "test", "NotAutoAcknowledgeble-NotLatchable-Critical->Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    )
  )

  @DataProvider(name = "acknowledgeAndLatching-data-provider")
  def acknowledgeAndLatchDataProvider: Array[AcknowledgeAndLatchTestCase] = {
    ackAndLatchTestCases
  }

  @Test(dataProvider = "acknowledgeAndLatching-data-provider")
  def acknowledgeAndLatchTest(testCase: AcknowledgeAndLatchTestCase): Unit = {
    //test setup
    alarmServiceImpl
      .setMetadata(
        testCase.alarmKey,
        AlarmMetadata(
          subsystem = testCase.alarmKey.subsystem,
          component = testCase.alarmKey.component,
          name = testCase.alarmKey.name,
          description = "for test purpose",
          location = "testing",
          AlarmType.Absolute,
          Set(Okay, Warning, Major, Indeterminate, Critical),
          probableCause = "test",
          operatorResponse = "test",
          isAutoAcknowledgeable = testCase.isAutoAcknowledgeble,
          isLatchable = testCase.isAlarmLachable,
          activationStatus = Active
        )
      )
      .await
    alarmServiceImpl
      .setStatus(
        testCase.alarmKey,
        AlarmStatus(
          latchedSeverity = testCase.oldSeverity,
          acknowledgementStatus =
            if (testCase.isAutoAcknowledgeble || testCase.oldSeverity == Okay) Acknowledged else UnAcknowledged,
          latchStatus = if (testCase.isAlarmLachable && testCase.newSeverity.latchable) Latched else UnLatched
        )
      )
      .await

    //set severity to new Severity
    val status = setSeverity(testCase.alarmKey, testCase.newSeverity)

    //get severity and assert
    status.acknowledgementStatus shouldEqual testCase.expectedAckStatus
    status.latchStatus shouldEqual testCase.expectedLatchStatus
  }

  @Test(dataProvider = "setSeverityTest-data-provider")
  def setSeverityTest(testCase: SetSeverityTestCase): Unit = {
    //set severity to new Severity
    val status = setSeverity(testCase.alarmKey, testCase.newSeverity)

    //get severity and assert
    status.latchedSeverity shouldEqual testCase.outcome
  }

  private def setSeverity(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    alarmService.setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
