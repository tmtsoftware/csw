package org.tmt.nfiraos.sampleassembly

import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue, UnsupportedCommandIssue}
import csw.params.commands.CommandResponse.{Accepted, Invalid, SubmitResponse, ValidateCommandResponse}
import csw.params.commands.{ControlCommand, Setup}
import csw.params.core.models.Id


object SampleValidation {

  import org.tmt.nfiraos.shared.SampleInfo._

  def doAssemblyValidation(runId: Id, command: ControlCommand): ValidateCommandResponse =
    command match {
      case s: Setup =>
        doAssemblySetupValidation(runId, s)
      case a =>
        Invalid(runId, UnsupportedCommandIssue("Sample assembly only supports Setup commands.") )
    }

   private def doAssemblySetupValidation(runId: Id, setup: Setup): ValidateCommandResponse =
    setup.commandName match {
      case `sleep` =>
        validateSleep(runId, setup)
      case `cancelLongCommand` =>
        validateCancel(runId, setup)
      case `immediateCommand` | `shortCommand` | `mediumCommand` | `longCommand` | `complexCommand` =>
        println("Got here")
        Accepted(runId)
      case _ =>
        Invalid(runId, UnsupportedCommandIssue(s"Command: ${setup.commandName.name} is not supported for sample Assembly."))
    }

  def doHcdValidation(runId: Id, command: ControlCommand): ValidateCommandResponse =
    command match {
      case s: Setup =>
        doHcdSetupValidation(runId, s)
      case a =>
        Invalid(runId, UnsupportedCommandIssue("Sample HCD only supports Setup commands.") )
    }

  private def doHcdSetupValidation(runId: Id, setup: Setup): ValidateCommandResponse =
    setup.commandName match {
      case `hcdSleep` =>
        validateSleep(runId, setup)
      case `hcdCancelLong` =>
        validateCancel(runId, setup)
      case `hcdShort` | `hcdMedium` | `hcdLong` =>
        println("Got here")
        Accepted(runId)
      case _ =>
        Invalid(runId, UnsupportedCommandIssue(s"Command: ${setup.commandName.name} is not supported for sample HCD."))
    }

  private def validateSleep(runId: Id, setup: Setup): ValidateCommandResponse =
    if (setup.exists(sleepTimeKey)) {
      val sleepTime: Long = setup(sleepTimeKey).head
      if (sleepTime < maxSleep)
        Accepted(runId)
      else
        Invalid(runId, ParameterValueOutOfRangeIssue("sleepTime must be < 2000"))
    }
    else {
      Invalid(runId, MissingKeyIssue(s"required sleep command key: $sleepTimeKey is missing."))
    }

  private def validateCancel(runId: Id, setup: Setup): ValidateCommandResponse =
    if (setup.exists(cancelKey)) {
      Accepted(runId)
    }
    else {
      Invalid(runId, MissingKeyIssue(s"required cancel command key: $cancelKey is missing."))
    }

}
