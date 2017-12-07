package csw.common.components.command

import csw.messages.params.generics.GChoiceKey
import csw.messages.params.generics.KeyType.ChoiceKey
import csw.messages.params.models.{Choice, Choices, Prefix}

object ComponentStateForCommand {
  val prefix               = Prefix("wfos.prog.cloudcover")
  val acceptedCmdPrefix    = Prefix("wfos.prog.cloudcover.accepted")
  val withoutMatcherPrefix = Prefix("wfos.prog.cloudcover.accept.result")
  val matcherPrefix        = Prefix("wfos.prog.cloudcover.accept.matcher.success.result")
  val immediateCmdPrefix   = Prefix("wfos.prog.cloudcover.immediate")
  val invalidCmdPrefix     = Prefix("wfos.prog.cloudcover.failure")

  val longRunningCmdPrefix   = Prefix("mcs.mobie.blue.longCmd")
  val shortRunningCmdPrefix  = Prefix("mcs.mobie.blue.shortCmd")
  val mediumRunningCmdPrefix = Prefix("mcs.mobie.blue.mediumCmd")

  val shortCmdCompleted  = Choice("Short Running Cmd Completed")
  val mediumCmdCompleted = Choice("Medium Running Cmd Completed")
  val longCmdCompleted   = Choice("Long Running Cmd Completed")

  val choices: Choices      = Choices.fromChoices(shortCmdCompleted, mediumCmdCompleted, longCmdCompleted)
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)
}
