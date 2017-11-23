package csw.common.components.command

import csw.messages.params.models.Prefix

object ComponentStateForCommand {
  val prefix               = Prefix("wfos.prog.cloudcover")
  val acceptedCmdPrefix    = Prefix("wfos.prog.cloudcover.success")
  val immediateCmdPrefix   = Prefix("wfos.prog.cloudcover.immediate")
  val invalidCmdPrefix     = Prefix("wfos.prog.cloudcover.failure")
  val longRunningCmdPrefix = Prefix("wfos.prog.cloudcover.longRunning")
  val lockPrefix           = Prefix("wfos.prog.cloudcover.lock")
}
