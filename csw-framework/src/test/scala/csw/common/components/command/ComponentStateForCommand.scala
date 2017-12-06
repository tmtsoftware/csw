package csw.common.components.command

import csw.messages.params.models.Prefix

object ComponentStateForCommand {
  val prefix                       = Prefix("wfos.prog.cloudcover")
  val acceptedCmdPrefix            = Prefix("wfos.prog.cloudcover.accepted")
  val acceptWithNoMatcherCmdPrefix = Prefix("wfos.prog.cloudcover.accept.result")
  val acceptWithMatcherCmdPrefix   = Prefix("wfos.prog.cloudcover.accept.matcher.success.result")
  val immediateCmdPrefix           = Prefix("wfos.prog.cloudcover.immediate")
  val invalidCmdPrefix             = Prefix("wfos.prog.cloudcover.failure")
  val cancellableCmdPrefix         = Prefix("wfos.prog.cloudcover.cancellableCmd")
  val cancelCmdPrefix              = Prefix("wfos.prog.cloudcover.cancelCmd")

  val longRunningCmdPrefix   = Prefix("mcs.mobie.blue.longCmd")
  val shortRunningCmdPrefix  = Prefix("mcs.mobie.blue.shortCmd")
  val mediumRunningCmdPrefix = Prefix("mcs.mobie.blue.mediumCmd")
}
