/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.helpers
import csw.alarm.models.ActivationStatus.{Active, Inactive}
import csw.alarm.models.AlarmSeverity.*
import csw.alarm.models.AlarmType.Absolute
import csw.alarm.models.Key.AlarmKey
import csw.alarm.models.{AlarmMetadata, AlarmType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{LGSF, NFIRAOS, TCS}

trait AlarmTestData {
  // latchable, not auto-acknowledgable alarm
  val tromboneAxisLowLimitAlarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisLowLimitAlarm")
  val tromboneAxisLowLimitAlarm = AlarmMetadata(
    prefix = Prefix(NFIRAOS, "trombone"),
    name = "tromboneAxisLowLimitAlarm",
    description = "Warns when trombone axis has reached the low limit",
    location = "south side",
    alarmType = AlarmType.Absolute,
    Set(Warning, Major, Critical, Indeterminate, Okay),
    probableCause = "the trombone software has failed or the stage was driven into the low limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = false,
    isLatchable = true,
    activationStatus = Active
  )

  // latchable and auto-Acknowledgeable alarm
  val tromboneAxisHighLimitAlarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")
  val tromboneAxisHighLimitAlarm = AlarmMetadata(
    prefix = Prefix(NFIRAOS, "trombone"),
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

  // Multiple alarms added - no auto ack no latch
  val splitterLimitAlarmKey = AlarmKey(Prefix(NFIRAOS, "beamsplitter"), "splitterLimitAlarm")
  val splitterLimitAlarm = AlarmMetadata(
    prefix = Prefix(NFIRAOS, "beamsplitter"),
    name = "splitterLimitAlarm",
    description = "Warns when beam splitter hits a limit",
    location = "south side",
    AlarmType.Absolute,
    Set(Indeterminate, Okay, Critical),
    probableCause = "the beam splitter has passed software limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum beamsplitter command",
    isAutoAcknowledgeable = false,
    isLatchable = false,
    activationStatus = Active
  )

  // Enclosure alarms added - no auto ack yes latch
  val enclosureTempHighAlarmKey = AlarmKey(Prefix(NFIRAOS, "enclosure"), "tempHighAlarm")
  val enclosureTempHighAlarm = AlarmMetadata(
    prefix = Prefix(NFIRAOS, "enclosure"),
    name = "tempHighAlarm",
    description = "Enclosure temperature has gone out of range high",
    location = "enclosure",
    AlarmType.Absolute,
    Set(Indeterminate, Okay, Critical),
    probableCause = "the temperature controller has failed",
    operatorResponse = "go to the NFIRAOS engineering user interface and begin the shutdown enclosure process",
    isAutoAcknowledgeable = false,
    isLatchable = true,
    activationStatus = Active
  )

  // Enclosure alarms added - no auto ack no latch
  val enclosureTempLowAlarmKey = AlarmKey(Prefix(NFIRAOS, "enclosure"), "tempLowAlarm")
  val enclosureTempLowAlarm = AlarmMetadata(
    prefix = Prefix(NFIRAOS, "enclosure"),
    name = "tempLowAlarm",
    description = "Enclosure temperature has gone out of range low",
    location = "enclosure",
    AlarmType.Absolute,
    Set(Indeterminate, Okay, Critical),
    probableCause = "the temperature controller has failed",
    operatorResponse = "go to the NFIRAOS engineering user interface and begin the shutdown enclosure process",
    isAutoAcknowledgeable = false,
    isLatchable = true,
    activationStatus = Inactive
  )

  // un-latchable, auto-acknowledgable alarm
  val cpuExceededAlarmKey = AlarmKey(Prefix(TCS, "tcspk"), "cpuExceededAlarm")
  val cpuExceededAlarm = AlarmMetadata(
    prefix = Prefix(TCS, "tcspk"),
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

  // un-latchable, auto-acknowledgable alarm
  val outOfRangeOffloadAlarmKey = AlarmKey(Prefix(TCS, "corrections"), "outOfRangeOffload")
  val outOfRangeOffloadAlarm = AlarmMetadata(
    prefix = Prefix(TCS, "corrections"),
    name = "outOfRangeOffload",
    description = "Another system has sent an out of range offload that has caused the system to go into a bad state!",
    location = "Computer Room",
    alarmType = Absolute,
    supportedSeverities = Set(Indeterminate, Okay, Warning, Major),
    probableCause = "Bad software in NFIRAOS or WFOS",
    operatorResponse = "Reset the software system and hope",
    isAutoAcknowledgeable = false,
    isLatchable = true,
    activationStatus = Active
  )

  val cpuIdleAlarmKey = AlarmKey(Prefix(LGSF, "tcspkinactive"), "cpuIdleAlarm")
  val cpuIdleAlarm = AlarmMetadata(
    prefix = Prefix(LGSF, "tcspkinactive"),
    name = "cpuIdleAlarm",
    description = "This alarm is activated CPU is idle",
    location = "in computer...",
    alarmType = Absolute,
    supportedSeverities = Set(Indeterminate, Okay, Warning, Major, Critical),
    probableCause = "too fast...",
    operatorResponse = "slow it down...",
    isAutoAcknowledgeable = true,
    isLatchable = false,
    activationStatus = Inactive
  )
}
