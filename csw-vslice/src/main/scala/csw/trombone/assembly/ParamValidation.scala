package csw.trombone.assembly

import csw.messages.ccs.CommandIssue._
import csw.messages.ccs.commands.CommandValidationResponse.{Accepted, Invalid}
import csw.messages.ccs.commands.{CommandValidationResponse, Setup}

import scala.util.Try

/**
 * TMT Source Code: 8/24/16.
 */
object ParamValidation {

  /**
   * Runs Trombone-specific validation on a single Setup.
   * @return
   */
  def validateOneSetup(s: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {
    s.prefix match {
      case ac.initCK         => initValidation(s)
      case ac.datumCK        => datumValidation(s)
      case ac.stopCK         => stopValidation(s)
      case ac.moveCK         => moveValidation(s)
      case ac.positionCK     => positionValidation(s)
      case ac.setElevationCK => setElevationValidation(s)
      case ac.setAngleCK     => setAngleValidation(s)
      case ac.followCK       => followValidation(s)
      case x                 => Invalid(s.runId, OtherIssue(s"Setup with prefix $x is not supported"))
    }
  }

  /**
   * CommandValidationResponse for the init Setup
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def initValidation(sc: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {

    val size = sc.size
    if (sc.prefix != ac.initCK) Invalid(sc.runId, WrongPrefixIssue("The Setup is not an init configuration"))
    else // If no arguments, then this is okay
    if (sc.size == 0)
      Accepted(sc.runId)
    else if (size == 2) {
      import ac._
      // Check for correct keys and types
      // This example assumes that we want only these two keys
      val missing = sc.missingKeys(configurationNameKey, configurationVersionKey)
      if (missing.nonEmpty)
        Invalid(sc.runId,
                MissingKeyIssue(
                  s"The 2 parameter init Setup requires keys: $configurationNameKey and $configurationVersionKey"
                ))
      else if (Try(sc(configurationNameKey)).isFailure || Try(sc(configurationVersionKey)).isFailure)
        Invalid(
          sc.runId,
          WrongParameterTypeIssue(
            s"The init Setup requires keys named: $configurationVersionKey and $configurationVersionKey of type GParam[String]"
          )
        )
      else Accepted(sc.runId)
    } else
      Invalid(sc.runId, WrongNumberOfParametersIssue(s"The init Setup requires 0 or 2 items, but $size were received"))
  }

  /**
   * CommandValidationResponse for the datum Setup -- currently nothing to validate
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def datumValidation(sc: Setup): CommandValidationResponse = Accepted(sc.runId)

  /**
   * CommandValidationResponse for the stop Setup -- currently nothing to validate
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def stopValidation(sc: Setup): CommandValidationResponse = Accepted(sc.runId)

  /**
   * CommandValidationResponse for the move Setup
   * Note: position is optional, if not present, it moves to home
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def moveValidation(sc: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {
    if (sc.prefix != ac.moveCK) {
      Invalid(sc.runId, WrongPrefixIssue("The Setup is not a move configuration."))
    } else if (sc.size == 0)
      Accepted(sc.runId)
    else {
      // Check for correct key and type -- only checks that essential key is present, not strict
      if (!sc.exists(ac.stagePositionKey)) {
        Invalid(sc.runId, MissingKeyIssue(s"The move Setup must have a DoubleParameter named: ${ac.stagePositionKey}"))
      } else if (Try(sc(ac.stagePositionKey)).isFailure)
        Invalid(sc.runId,
                WrongParameterTypeIssue(s"The move Setup must have a DoubleParameter named: ${ac.stagePositionKey}"))
      else if (sc(ac.stagePositionKey).units != ac.stagePositionUnits) {
        Invalid(sc.runId,
                WrongUnitsIssue(
                  s"The move Setup parameter: ${ac.stagePositionKey} must have units of: ${ac.stagePositionUnits}"
                ))
      } else Accepted(sc.runId)
    }
  }

  /**
   * CommandValidationResponse for the position Setup -- must have a single parameter named rangeDistance
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def positionValidation(sc: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {
    if (sc.prefix != ac.positionCK) {
      Invalid(sc.runId, WrongPrefixIssue("The Setup is not a position configuration."))
    } else {
      // The spec says parameter is not required, but doesn't explain so requiring parameter
      // Check for correct key and type -- only checks that essential key is present, not strict
      if (!sc.exists(ac.naRangeDistanceKey)) {
        Invalid(sc.runId,
                MissingKeyIssue(s"The position Setup must have a DoubleParameter named: ${ac.naRangeDistanceKey}"))
      } else if (Try(sc(ac.naRangeDistanceKey)).isFailure) {
        Invalid(
          sc.runId,
          WrongParameterTypeIssue(s"The position Setup must have a DoubleParameter named: ${ac.naRangeDistanceKey}")
        )
      } else if (sc(ac.naRangeDistanceKey).units != ac.naRangeDistanceUnits) {
        Invalid(
          sc.runId,
          WrongUnitsIssue(
            s"The position Setup parameter: ${ac.naRangeDistanceKey} must have units of: ${ac.naRangeDistanceUnits}"
          )
        )
      } else {
        val el = sc(ac.naRangeDistanceKey).head
        if (el < 0) {
          Invalid(sc.runId,
                  ParameterValueOutOfRangeIssue(
                    s"Range distance value of $el for position must be greater than or equal 0 km."
                  ))
        } else Accepted(sc.runId)
      }
    }
  }

  /**
   * CommandValidationResponse for the setElevation Setup
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def setElevationValidation(sc: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {
    if (sc.prefix != ac.setElevationCK) {
      Invalid(sc.runId, WrongPrefixIssue("The Setup is not a setElevation configuration"))
    } else if (sc.missingKeys(ac.naElevationKey).nonEmpty) {
      // Check for correct key and type -- only checks that essential key is present, not strict
      Invalid(sc.runId, MissingKeyIssue(s"The setElevation Setup must have a parameter named: ${ac.naElevationKey}"))
    } else if (Try(sc(ac.naElevationKey)).isFailure) {
      Invalid(sc.runId,
              WrongParameterTypeIssue(
                s"The setElevation Setup must have a parameter: ${ac.naElevationKey} as a DoubleParameter"
              ))
    } else if (sc(ac.naElevationKey).units != ac.naRangeDistanceUnits) {
      Invalid(
        sc.runId,
        WrongUnitsIssue(s"The move Setup parameter: ${ac.naElevationKey} must have units: ${ac.naElevationUnits}")
      )
    } else Accepted(sc.runId)
  }

  /**
   * CommandValidationResponse for the setAngle Setup
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def setAngleValidation(sc: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {
    if (sc.prefix != ac.setAngleCK) {
      Invalid(sc.runId, WrongPrefixIssue("The Setup is not a setAngle configuration"))
    } else // Check for correct key and type -- only checks that essential key is present, not strict
    if (!sc.exists(ac.zenithAngleKey)) {
      Invalid(sc.runId, MissingKeyIssue(s"The setAngle Setup must have a DoubleParameter named: ${ac.zenithAngleKey}"))
    } else if (Try(sc(ac.zenithAngleKey)).isFailure) {
      Invalid(sc.runId,
              WrongParameterTypeIssue(s"The setAngle Setup must have a DoubleParameter named: ${ac.zenithAngleKey}"))
    } else if (sc(ac.zenithAngleKey).units != ac.zenithAngleUnits) {
      Invalid(
        sc.runId,
        WrongUnitsIssue(s"The setAngle Setup parameter: ${ac.zenithAngleKey} must have units: ${ac.zenithAngleUnits}")
      )
    } else Accepted(sc.runId)
  }

  /**
   * CommandValidationResponse for the follow Setup
   * @param sc the received Setup
   * @return Accepted or Invalid
   */
  def followValidation(sc: Setup)(implicit ac: AssemblyContext): CommandValidationResponse = {
    if (sc.prefix != ac.followCK) {
      Invalid(sc.runId, WrongPrefixIssue("The Setup is not a follow configuration"))
    } else if (!sc.exists(ac.nssInUseKey)) {
      // Check for correct key and type -- only checks that essential key is present, not strict
      Invalid(sc.runId, MissingKeyIssue(s"The follow Setup must have a BooleanParameter named: ${ac.nssInUseKey}"))
    } else if (Try(sc(ac.nssInUseKey)).isFailure) {
      Invalid(sc.runId,
              WrongParameterTypeIssue(s"The follow Setup must have a BooleanParameter named ${ac.nssInUseKey}"))
    } else Accepted(sc.runId)
  }
}
