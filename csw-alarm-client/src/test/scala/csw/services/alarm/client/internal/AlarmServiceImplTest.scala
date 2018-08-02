package csw.services.alarm.client.internal
import java.io.File

import csw.services.alarm.api.exceptions.InvalidSeverityException
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.UnShelved
import csw.services.alarm.api.models.{AlarmSeverity, AlarmStatus}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

class AlarmServiceImplTest extends AlarmServiceTestSetup(26381, 6381) {

  override protected def beforeEach(): Unit = {
    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val file = new File(path)
    alarmService.initAlarms(file, reset = true).await
  }

  test("test set severity") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    //set severity to Major
    val status = setSeverity(tromboneAxisHighLimitAlarm, Major)
    status shouldEqual AlarmStatus(Acknowledged, Latched, Major, UnShelved)

    //get severity and assert
    val alarmSeverity = alarmServiceFactory.severityApi.get(tromboneAxisHighLimitAlarm).await.get
    alarmSeverity shouldEqual Major

    //wait for 1 second and assert expiry of severity
    Thread.sleep(1000)
    val severityAfter1Second = alarmService.getSeverity(tromboneAxisHighLimitAlarm).await
    severityAfter1Second shouldEqual Disconnected
  }

  test("should throw InvalidSeverityException when unsupported severity is provided") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    intercept[InvalidSeverityException] {
      alarmService.setSeverity(tromboneAxisHighLimitAlarm, AlarmSeverity.Critical).await
    }
  }

  test("should not latch the alarm when it's latchable but not high risk") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    //set severity to Okay
    val status = setSeverity(tromboneAxisHighLimitAlarm, Okay)
    status shouldEqual AlarmStatus(latchStatus = UnLatched, latchedSeverity = Okay)

    //set severity to indeterminant
    val status1 = setSeverity(tromboneAxisHighLimitAlarm, Indeterminate)
    status1 shouldEqual AlarmStatus(latchStatus = UnLatched, latchedSeverity = Indeterminate)
  }

  test("should latch alarm only when it is high risk and higher than latched severity in case of latchable alarms") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    val status                     = setSeverity(tromboneAxisHighLimitAlarm, Major)

    status shouldEqual AlarmStatus(acknowledgementStatus = Acknowledged, latchStatus = Latched, latchedSeverity = Major)

    val status1 = setSeverity(tromboneAxisHighLimitAlarm, Warning)
    status1 shouldEqual AlarmStatus(acknowledgementStatus = Acknowledged, latchStatus = Latched, latchedSeverity = Major)

    val status2 = setSeverity(tromboneAxisHighLimitAlarm, Okay)
    status2 shouldEqual AlarmStatus(acknowledgementStatus = Acknowledged, latchStatus = Latched, latchedSeverity = Major)
  }

  test("should not latch alarm if it is not latchable") {
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")
    val status           = setSeverity(cpuExceededAlarm, Critical)
    status shouldEqual AlarmStatus(acknowledgementStatus = Acknowledged, latchStatus = UnLatched, latchedSeverity = Critical)

    val status1 = setSeverity(cpuExceededAlarm, Indeterminate)
    status1 shouldEqual AlarmStatus(acknowledgementStatus = Acknowledged,
                                    latchStatus = UnLatched,
                                    latchedSeverity = Indeterminate)
  }

  test("should auto-acknowledge alarm only when it is auto-acknowledgable while setting severity") {
    val tromboneAxisLowLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
    val status                    = setSeverity(tromboneAxisLowLimitAlarm, Major)
    status shouldEqual AlarmStatus(acknowledgementStatus = UnAcknowledged, latchStatus = Latched, latchedSeverity = Major)
  }

//  test("should throw exception while getting severity if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.getSeverity(invalidAlarm)
//    }
//  }
//
//  test("should throw exception while getting status if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.getStatus(invalidAlarm)
//    }
//  }
//
//  test("should throw exception while getting metadata if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.getMetadata(invalidAlarm)
//    }
//  }
//
//  test("should throw exception if no alarms are found while getting metadata by subsystem") {
//    val invalidAlarm = SubsystemKey("invalid")
//    intercept[NoAlarmsFoundException] {
//      alarmService.getMetadata(invalidAlarm)
//    }
//  }
//
//  test("should fetch all alarms for a subsystem") {
//    val nfiraosKey = SubsystemKey("nfiraos")
//    alarmService.getMetadata(nfiraosKey)
//  }
//
//  test("should fetch all alarms for a component") {
//    val tromboneKey = ComponentKey("nfiraos", "trombone")
//    alarmService.getMetadata(tromboneKey)
//  }
//
//  test("should fetch all alarms of whole system") {
//    val globalKey = GlobalKey
//    alarmService.getMetadata(globalKey)
//  }
//
//  test("should throw exception while acknowledging alarm if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.acknowledge(invalidAlarm)
//    }
//  }
//
//  test("should acknowledge an alarm") {}
//
//  test("should throw exception while resetting alarm if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.reset(invalidAlarm)
//    }
//  }
//
//  test("should throw exception while resetting alarm if severity is not okay") {
//    val tromboneAxisLowLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
//    intercept[ResetOperationNotAllowed] {
//      alarmService.reset(tromboneAxisLowLimitAlarm)
//    }
//  }
  private def setSeverity(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    alarmService.setSeverity(alarmKey, alarmSeverity).await
    alarmServiceFactory.statusApi.get(alarmKey).await.get
  }
}
