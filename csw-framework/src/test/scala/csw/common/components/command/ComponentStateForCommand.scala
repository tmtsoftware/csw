package csw.common.components.command

import csw.messages.ccs.commands.CommandName
import csw.messages.params.generics.GChoiceKey
import csw.messages.params.generics.KeyType.ChoiceKey
import csw.messages.params.models.{Choice, Choices, Prefix}

object ComponentStateForCommand {
  val prefix        = Prefix("wfos.prog.cloudcover")
  val invalidPrefix = Prefix("wfos.prog.cloudcover.invalid")

  val moveCmd           = CommandName("move")
  val initCmd           = CommandName("init")
  val acceptedCmd       = CommandName("move.accepted")
  val withoutMatcherCmd = CommandName("move.accept.result")
  val matcherCmd        = CommandName("move.accept.matcher.success.result")
  val immediateCmd      = CommandName("move.immediate")
  val immediateResCmd   = CommandName("move.immediate.result")
  val invalidCmd        = CommandName("move.failure")
  val cancelCmd         = CommandName("move.cancel")

  val longRunning   = CommandName("move.longCmd")
  val shortRunning  = CommandName("move.shortCmd")
  val mediumRunning = CommandName("move.mediumCmd")

  val shortCmdCompleted  = Choice("Short Running Cmd Completed")
  val mediumCmdCompleted = Choice("Medium Running Cmd Completed")
  val longCmdCompleted   = Choice("Long Running Cmd Completed")

  val choices: Choices      = Choices.fromChoices(shortCmdCompleted, mediumCmdCompleted, longCmdCompleted)
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)
}
