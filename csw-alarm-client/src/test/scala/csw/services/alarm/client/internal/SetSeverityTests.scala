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
import csw.services.alarm.client.internal.helpers.{AcknowledgeAndLatchTestCase, AlarmServiceTestSetup, SetSeverityTestCase}

//DEOPSCSW-444 : Set severity api for component
class SetSeverityTests extends AlarmServiceTestSetup {

  val `low_latchable=>high_latchable` = AlarmKey("AOESW", "test_component", "low_latchable=>high_latchable")
  val `low_unlatable=>hig_latchable`  = AlarmKey("AOESW", "test_component", "low_unlatable=>hig_latchable")
  val `high_latchable=>low_latchable` = AlarmKey("AOESW", "test_component", "high_latchable=>low_latchable")
  val `high_unlatable=>low_latchable` = AlarmKey("AOESW", "test_component", "high_unlatable=>low_latchable")

  val alarmServiceImpl: AlarmServiceImpl = alarmService.asInstanceOf[AlarmServiceImpl]

  override def beforeAll() {
    val validAlarmsFile   = new File(getClass.getResource("/test-alarms/latchSeverityTestAlarms.conf").getPath)
    val validAlarmsConfig = ConfigFactory.parseFile(validAlarmsFile).resolve(ConfigResolveOptions.noSystem())
    alarmService.initAlarms(validAlarmsConfig, reset = true).await

    List(
      (`low_latchable=>high_latchable`, Warning),
      (`low_unlatable=>hig_latchable`, Disconnected),
      (`high_latchable=>low_latchable`, Major),
      (`high_unlatable=>low_latchable`, Disconnected)
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

  val setSeverityTestDataProvider: Array[SetSeverityTestCase] = Array(
    SetSeverityTestCase(
      oldSeverity = Warning,
      newSeverity = Major,
      outcome = Major,
      alarmKey = `low_latchable=>high_latchable`
    ),
    SetSeverityTestCase(
      oldSeverity = Disconnected,
      newSeverity = Critical,
      outcome = Critical,
      alarmKey = `low_unlatable=>hig_latchable`
    ),
    SetSeverityTestCase(
      oldSeverity = Major,
      newSeverity = Warning,
      outcome = Major,
      alarmKey = `high_latchable=>low_latchable`
    ),
    SetSeverityTestCase(
      oldSeverity = Disconnected,
      newSeverity = Major,
      outcome = Major,
      alarmKey = `high_unlatable=>low_latchable`
    )
  )

  val ackAndLatchTestCases = Array(
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "AutoAcknowledgeble_Latchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "NotAutoAcknowledgeble_Latchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "AutoAcknowledgeble_NotLatchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "NotAutoAcknowledgeble_NotLatchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "AutoAcknowledgeble_Latchable_Okay=>Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "NotAutoAcknowledgeble_Latchable_Okay=>Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "AutoAcknowledgeble_NotLatchable_Okay=>Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "NotAutoAcknowledgeble_NotLatchable_Okay=>Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "AutoAcknowledgeble_Latchable_Critical=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "NotAutoAcknowledgeble_Latchable_Critical=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "AutoAcknowledgeble_NotLatchable_Critical=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey("AOESW", "test", "NotAutoAcknowledgeble_NotLatchable_Critical=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    )
  )

  ackAndLatchTestCases.foreach(
    testCase =>
      test(testCase.toString) {
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
              latchStatus = if (testCase.isAlarmLachable && testCase.oldSeverity.latchable) Latched else UnLatched
            )
          )
          .await

        //set severity to new Severity
        val status = setSeverity(testCase.alarmKey, testCase.newSeverity)

        //get severity and assert
        status.acknowledgementStatus shouldEqual testCase.expectedAckStatus
        status.latchStatus shouldEqual testCase.expectedLatchStatus
    }
  )

  setSeverityTestDataProvider.foreach(
    testCase =>
      test(testCase.toString) {
        //set severity to new Severity
        val status = setSeverity(testCase.alarmKey, testCase.newSeverity)

        //get severity and assert
        status.latchedSeverity shouldEqual testCase.outcome
    }
  )

  private def setSeverity(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    alarmService.setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
