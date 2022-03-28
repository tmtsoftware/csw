/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tmt.csw.sample

import csw.params.commands.CommandName
import csw.params.core.generics.KeyType.{ChoiceKey, StringKey}
import csw.params.core.generics.{GChoiceKey, Key, KeyType, Parameter}
import csw.params.core.models.{Choice, Choices}
import csw.params.events.EventName
import csw.prefix.models.Prefix

object ComponentStateForCommand {
  val encoder: Key[Int]       = KeyType.IntKey.make("encoder")
  val seqPrefix: Prefix       = Prefix("wfos.seq")
  val filterAsmPrefix: Prefix = Prefix("wfos.blue.filter")
//  val filterHcdPrefix       = Prefix("wfos.blue.filter.hcd")
  val prefix: Prefix = Prefix("wfos.blue.filter")
//  val invalidPrefix: Prefix = Prefix("wfos.blue.filter.invalid")

//  val moveCmd: CommandName                   = CommandName("move")
  val initCmd: CommandName           = CommandName("init")
  val acceptedCmd: CommandName       = CommandName("move.accepted")
  val longRunningCmd: CommandName    = CommandName("move.accept.result")
  val onewayCmd: CommandName         = CommandName("move.oneway.accept")
  val matcherCmd: CommandName        = CommandName("move.accept.matcher.success.result")
  val matcherFailedCmd: CommandName  = CommandName("move.accept.matcher.failed.result")
  val matcherTimeoutCmd: CommandName = CommandName("move.accept.matcher.success.timeout")
//  val assemCurrentStateCmd: CommandName      = CommandName("assem.send.current.state")
  val hcdCurrentStateCmd: CommandName = CommandName("hcd.send.current.state")
  val crmAddOrUpdateCmd: CommandName  = CommandName("hcd.update.crm")
  val immediateCmd: CommandName       = CommandName("move.immediate")
  val immediateResCmd: CommandName    = CommandName("move.immediate.result")
  val invalidCmd: CommandName         = CommandName("move.failure")
//  val cancelCmd: CommandName                 = CommandName("move.cancel")
  val failureAfterValidationCmd: CommandName = CommandName("move.accept.failure")

  val longRunning: CommandName   = CommandName("move.longCmd")
  val shortRunning: CommandName  = CommandName("move.shortCmd")
  val mediumRunning: CommandName = CommandName("move.mediumCmd")

  val longRunningCmdToHcd: CommandName        = CommandName("move.accept.toHCD")
  val longRunningCmdToAsm: CommandName        = CommandName("move.accept.toAsm")
  val longRunningCmdToAsmInvalid: CommandName = CommandName("move.accept.toAsmError")
  val longRunningCmdToAsmComp: CommandName    = CommandName("move.accept.toAsmWithCompleter")
  val longRunningCmdToAsmCActor: CommandName  = CommandName("move.accept.toAsmWithCompleterActor")
  val cmdWithBigParameter: CommandName        = CommandName("complex.command.parameters")
  val shorterHcdCmd: CommandName              = CommandName("move.accept.shorterInHcd")
  val shorterHcdErrorCmd: CommandName         = CommandName("move.accept.shorterErrorInHcd")

  val longRunningCmdCompleted: Choice  = Choice("Long Running Cmd Completed")
  val longRunningCurrentStatus: Choice = Choice("Long Running Cmd Completed")
  val shortCmdCompleted: Choice        = Choice("Short Running Sub Cmd Completed")
  val mediumCmdCompleted: Choice       = Choice("Medium Running Sub Cmd Completed")
  val longCmdCompleted: Choice         = Choice("Long Running Sub Cmd Completed")
  val onlineChoice: Choice             = Choice("Online")
  val shutdownChoice: Choice           = Choice("Shutdown")
  val initChoice: Choice               = Choice("Initialize")
  val offlineChoice: Choice            = Choice("Offline")
  val choices: Choices = Choices.fromChoices(
    shortCmdCompleted,
    mediumCmdCompleted,
    longCmdCompleted,
    longRunningCmdCompleted,
    longRunningCurrentStatus,
    onlineChoice,
    shutdownChoice,
    initChoice,
    offlineChoice
  )
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)

  val diagnosticDataEventName: EventName     = EventName("diagnostic-data")
  val diagnosticModeParam: Parameter[String] = StringKey.make("diagnostic-data").set("diagnostic-publish")
}
