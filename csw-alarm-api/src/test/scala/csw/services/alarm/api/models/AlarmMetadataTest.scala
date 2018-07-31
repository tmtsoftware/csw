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

}
