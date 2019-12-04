package csw.common.components.command

import csw.params.commands.CommandName
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.generics.GChoiceKey
import csw.params.core.models.{Choice, Choices}
import csw.prefix.Prefix

object CommandComponentState {
  val seqPrefix       = Prefix("wfos.seq")
  val filterAsmPrefix = Prefix("wfos.blue.filter")
  val filterHcdPrefix = Prefix("wfos.blue.filter.hcd")

  val immediateCmd = CommandName("move.immediate")
  val invalidCmd   = CommandName("move.failure")

  val longRunningCmd             = CommandName("move.accept.timer")
  val longRunningCmdToHcd        = CommandName("move.accept.toHCD")
  val longRunningCmdToAsm        = CommandName("move.accept.toAsm")
  val longRunningCmdToAsmInvalid = CommandName("move.accept.toAsmError")
  val longRunningCmdToAsmComp    = CommandName("move.accept.toAsmWithCompleter")
  val longRunningCmdToAsmCActor  = CommandName("move.accept.toAsmWithCompleterActor")
  val cmdWithBigParameter        = CommandName("complex.command.parameters")
  val shorterHcdCmd              = CommandName("move.accept.shorterInHcd")
  val shorterHcdErrorCmd         = CommandName("move.accept.shorterErrorInHcd")

  val onlineChoice   = Choice("Online")
  val shutdownChoice = Choice("Shutdown")
  val initChoice     = Choice("Initialize")
  val offlineChoice  = Choice("Offline")

  val choices: Choices =
    Choices.fromChoices(
      onlineChoice,
      shutdownChoice,
      initChoice,
      offlineChoice
    )
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)
}
