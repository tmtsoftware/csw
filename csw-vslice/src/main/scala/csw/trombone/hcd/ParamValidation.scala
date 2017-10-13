package csw.trombone.hcd

import csw.messages.ccs.Validation
import csw.messages.ccs.Validations.Valid
import csw.messages.ccs.commands.{Observe, Setup}

object ParamValidation {
  def validateSetup(s: Setup): Validation     = Valid
  def validateObserve(s: Observe): Validation = Valid
}
