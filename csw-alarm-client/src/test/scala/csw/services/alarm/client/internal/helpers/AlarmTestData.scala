package csw.services.alarm.client.internal.helpers
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.{AlarmMetadata, AlarmType}
import csw.services.alarm.api.models.AlarmType.Absolute
import csw.services.alarm.api.models.Key.AlarmKey

trait AlarmTestData {

  // latchable and auto-Acknowledgeable alarm
  val tromboneAxisHighLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
  val tromboneAxisHighLimitAlarm = AlarmMetadata(
    subsystem = "NFIRAOS",
    component = "trombone",
    name = "tromboneAxisHighLimitAlarm",
    description = "Warns when trombone axis has reached the high limit",
    location = "south side",
    AlarmType.Absolute,
    Set(Indeterminate, Okay, Warning, Major),
    probableCause = "the trombone software has failed or the stage was driven into the high limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = true,
    isLatchable = true,
    activationStatus = Active
  )

  // latchable, not auto-acknowledgable alarm
  val tromboneAxisLowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
  val tromboneAxisLowLimitAlarm = AlarmMetadata(
    subsystem = "NFIRAOS",
    component = "trombone",
    name = "tromboneAxisLowLimitAlarm",
    description = "Warns when trombone axis has reached the low limit",
    location = "south side",
    alarmType = AlarmType.Absolute,
    Set(Warning, Major, Critical, Indeterminate, Okay),
    probableCause = "the trombone software has failed or the stage was driven into the low limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = false,
    isLatchable = true,
    activationStatus = Active,
  )

  // un-latchable, auto-acknowledgable alarm
  val cpuExceededAlarmKey = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")
  val cpuExceededAlarm = AlarmMetadata(
    subsystem = "TCS",
    component = "tcsPk",
    name = "cpuExceededAlarm",
    description =
      "This alarm is activated when the tcsPk Assembly can no longer calculate all of its pointing values in the time allocated. The CPU may lock power, or there may be pointing loops running that are not needed. Response: Check to see if pointing loops are executing that are not needed or see about a more powerful CPU.",
    location = "in computer...",
    alarmType = Absolute,
    supportedSeverities = Set(Indeterminate, Okay, Warning, Major, Critical),
    probableCause = "too fast...",
    operatorResponse = "slow it down...",
    isAutoAcknowledgeable = true,
    isLatchable = false,
    activationStatus = Active
  )

}
