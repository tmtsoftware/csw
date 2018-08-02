package csw.services.alarm.api.models
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-452: Represent alarm in a model as defined in Configuration file
class AlarmMetadataTest extends FunSuite with Matchers {

  val alarmMetadata = AlarmMetadata(
    subsystem = "nfiraos",
    component = "trombone",
    name = "tromboneAxisHighLimitAlarm",
    description = "Warns when trombone axis has reached the low limit",
    location = "south side",
    AlarmType.Absolute,
    Set(Warning, Major, Critical),
    probableCause = "the trombone software has failed or the stage was driven into the low limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = true,
    isLatchable = true,
    activationStatus = Active
  )

  // DEOPSCSW-435: Identify Alarm by Subsystem, component and AlarmName
  test("should get alarm key from AlarmMetadata") {
    alarmMetadata.alarmKey shouldBe AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
  }

  test("should always support Indeterminate and Okay severities ") {
    alarmMetadata.allSupportedSeverities shouldEqual Set(Indeterminate, Okay, Warning, Major, Critical)
  }

  test("should tell if the alarm is active or not") {
    alarmMetadata.isActive shouldBe true

    // for Inactive activation status
    alarmMetadata.copy(activationStatus = Inactive).isActive shouldBe false
  }

  test("should be able to get description of the alarm") {
    alarmMetadata.description shouldBe "Warns when trombone axis has reached the low limit"
  }

  test("should be able to get alarm type") {
    AlarmType.values should contain allElementsOf Set(alarmMetadata.alarmType)
  }

  // DEOPSCSW-453: Identify Probable cause of alarm
  test("should get probable cause from AlarmMetadata") {
    alarmMetadata.probableCause shouldBe "the trombone software has failed or the stage was driven into the low limit"
  }

  // DEOPSCSW-454: Instructions for corrective actions to handle the alarm
  test("should get operator response from AlarmMetadata") {
    alarmMetadata.operatorResponse shouldBe "go to the NFIRAOS engineering user interface and select the datum axis command"
  }

  // DEOPSCSW-455: Identify Alarm auto acknowledgement
  test("should be able to determine if alarm can be auto-acknowledged") {
    alarmMetadata.isAutoAcknowledgeable shouldBe true
  }

  // DEOPSCSW-456: Examine alarm latching state of each alarm
  test("should be able to determine if alarm can be latched") {
    alarmMetadata.isLatchable shouldBe true
  }
}
