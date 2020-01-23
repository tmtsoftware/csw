package org.tmt.esw.basic.shared

import csw.params.commands.CommandName
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.Units
import csw.prefix.models.Prefix

object SampleInfo {

  val testPrefix: Prefix = Prefix("ESW.test")

  // Sleep periods in milliseconds for short, medium, and long commands
  val shortSleepPeriod: Long  = 600L
  val mediumSleepPeriod: Long = 2000L
  val longSleepPeriod: Long   = 4000L

  // AssemblyCommands
  val sleep: CommandName            = CommandName("sleep")
  val immediateCommand: CommandName = CommandName("immediateCommand")
  val shortCommand: CommandName     = CommandName("shortCommand")
  val mediumCommand: CommandName    = CommandName("mediumCommand")
  val longCommand: CommandName      = CommandName("longCommand")
  val complexCommand: CommandName   = CommandName("complexCommand")
  val badCommand: CommandName       = CommandName("badCommand")

  // Command parameters and helpers
  val maxSleep: Long          = 5000
  val sleepTimeKey: Key[Long] = KeyType.LongKey.make("sleepTime")
  // Helper to get units set
  def setSleepTime(milli: Long): Parameter[Long] = sleepTimeKey.set(milli).withUnits(Units.millisecond)

  val resultKey: Key[Long] = KeyType.LongKey.make("result")

  // HCD Commands
  val hcdSleep: CommandName = CommandName("hcdSleep")
}
