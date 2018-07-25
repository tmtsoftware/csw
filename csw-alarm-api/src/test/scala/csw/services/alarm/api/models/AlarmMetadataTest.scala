package csw.services.alarm.api.models
import csw.services.alarm.api.models.AlarmSeverity._
import org.scalatest.{FunSuite, Matchers}

class AlarmMetadataTest extends FunSuite with Matchers {

  val alarmMetadata = AlarmMetadata(
    subsystem = "nfiraos",
    component = "cc.trombone",
    name = "tromboneAxisHighLimitAlarm",
    description = "Warns when trombone axis has reached the low limit",
    location = "south side",
    AlarmType.Absolute,
    List(Indeterminate, Okay, Warning, Major, Critical),
    probableCause = "the trombone software has failed or the stage was driven into the low limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = true,
    isLatchable = true
  )

  test("should get alarm key from AlarmMetadata") {
    alarmMetadata.alarmKey shouldBe AlarmKey("nfiraos", "cc.trombone", "tromboneAxisHighLimitAlarm")
  }

}
