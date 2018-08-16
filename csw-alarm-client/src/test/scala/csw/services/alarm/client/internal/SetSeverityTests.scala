package csw.services.alarm.client.internal

import csw.messages.params.models.Subsystem.AOESW
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

  val alarmServiceImpl: AlarmServiceImpl = alarmService.asInstanceOf[AlarmServiceImpl]

  val setSeverityTestCases: Array[SetSeverityTestCase] = Array(
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "low_latchable=>high_latchable"),
      oldSeverity = Warning,
      newSeverity = Major,
      expectedLatchedSeverity = Major
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "low_unlatable=>hig_latchable"),
      oldSeverity = Disconnected,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "high_latchable=>low_latchable"),
      oldSeverity = Major,
      newSeverity = Warning,
      expectedLatchedSeverity = Major
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "high_unlatable=>low_latchable"),
      oldSeverity = Disconnected,
      newSeverity = Major,
      expectedLatchedSeverity = Major
    )
  )

  setSeverityTestCases.foreach(
    testCase =>
      test(testCase.toString) {

        // Adding metadata for corresponding test in alarm store
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
              isAutoAcknowledgeable = true,
              isLatchable = true,
              activationStatus = Active
            )
          )
          .await

        // Adding status for corresponding test in alarm store
        (testCase.alarmKey, testCase.oldSeverity) match {
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

        //set severity to new Severity
        val status = setSeverity(testCase.alarmKey, testCase.newSeverity)

        //get severity and assert
        status.latchedSeverity shouldEqual testCase.expectedLatchedSeverity
    }
  )

  val ackAndLatchTestCases = Array(
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_Latchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_Latchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_NotLatchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_NotLatchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_Latchable_Okay=>Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_Latchable_Okay=>Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_NotLatchable_Okay=>Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_NotLatchable_Okay=>Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_Latchable_Critical=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_Latchable_Critical=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = UnAcknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_NotLatchable_Critical=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_NotLatchable_Critical=>Okay"),
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

  private def setSeverity(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    alarmService.setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
