package csw.trombone.hcd

import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.ccs.commands.{CommandResponse, Observe, Setup}

object ParamValidation {
  def validateSetup(s: Setup): CommandResponse     = Accepted(s.runId)
  def validateObserve(s: Observe): CommandResponse = Accepted(s.runId)
}
