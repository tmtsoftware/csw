package csw.trombone.hcd

import csw.messages.CommandValidationResponse
import csw.messages.CommandValidationResponse.Accepted
import csw.messages.ccs.commands.{Observe, Setup}

object ParamValidation {
  def validateSetup(s: Setup): CommandValidationResponse     = Accepted(s.runId)
  def validateObserve(s: Observe): CommandValidationResponse = Accepted(s.runId)
}
