package csw.common.components.command

import csw.messages.params.models.Prefix

object ComponentStateForCommand {
  val prefix                       = Prefix("wfos.prog.cloudcover")
  val acceptedCmdPrefix            = Prefix("wfos.prog.cloudcover.accepted")
  val acceptWithNoMatcherCmdPrefix = Prefix("wfos.prog.cloudcover.accept.result")
  val acceptWithMatcherCmdPrefix   = Prefix("wfos.prog.cloudcover.accept.matcher.result")
  val immediateCmdPrefix           = Prefix("wfos.prog.cloudcover.immediate")
  val invalidCmdPrefix             = Prefix("wfos.prog.cloudcover.failure")

  val longRunningCmdPrefix   = Prefix("mcs.mobie.blue.longCmd")
  val shortRunningCmdPrefix  = Prefix("mcs.mobie.blue.shortCmd")
  val mediumRunningCmdPrefix = Prefix("mcs.mobie.blue.mediumCmd")
}
