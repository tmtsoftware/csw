package org.tmt.nfiraos.shared

import csw.params.commands.CommandName
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{Id, Units}
import csw.prefix.models.Prefix

object SampleInfo {

  val testPrefix: Prefix     = Prefix("ESW.test")
  val assemblyPrefix: Prefix = Prefix("nfiraos.sampleassembly")
  val hcdPrefix: Prefix      = Prefix("nfiraos.samplehcd")

  // AssemblyCommands
  val sleep: CommandName             = CommandName("sleep")
  val immediateCommand: CommandName  = CommandName("immediateCommand")
  val shortCommand: CommandName      = CommandName("shortCommand")
  val mediumCommand: CommandName     = CommandName("mediumCommand")
  val longCommand: CommandName       = CommandName("longCommand")
  val cancelLongCommand: CommandName = CommandName("cancelLongCommand")
  val cancelWorker: CommandName      = CommandName("cancelWorkder")
  val complexCommand: CommandName    = CommandName("complexCommand")
  val sendToLocked: CommandName      = CommandName("sendToLocked")

  val maxSleep: Long          = 5000
  val sleepTimeKey: Key[Long] = KeyType.LongKey.make("sleepTime")
  // Helper to get units set
  def setSleepTime(milli: Long): Parameter[Long] = sleepTimeKey.set(milli).withUnits(Units.millisecond)

  val resultKey: Key[Long] = KeyType.LongKey.make("result")

  // For cancelling sleep
  val cancelKey: Key[String]                       = KeyType.StringKey.make("runId")
  def setCancelRunId(runId: Id): Parameter[String] = cancelKey.set(runId.id)

  // HCD Commands
  val hcdSleep: CommandName      = CommandName("hcdSleep")
  val hcdShort: CommandName      = CommandName("hcdShort")
  val hcdMedium: CommandName     = CommandName("hcdMedium")
  val hcdLong: CommandName       = CommandName("hcdLong")
  val hcdCancelLong: CommandName = CommandName("hcdCancelLong")
}
