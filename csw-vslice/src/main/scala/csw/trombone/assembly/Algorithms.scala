package csw.trombone.assembly

import csw.param.parameters.primitives.DoubleParameter
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}

/**
 * This object contains functions that implement this test version of the trombone assembly algorithms.
 *
 * These algorithms are representative in spirit, but not technically correct. The actual code will be
 * a closed loop and continually recalculate the elevation. Since the correctness of the algorithms was
 * not really the focus of the vertical slice, I wanted to just get something that sort of did the
 * right things and had the same variables.
 *
 * The algorithms here are implemented in an object so they can be tested independently.  I think this is
 * a good strategy.  Then they are called from the FollowActor and other places. The method names are
 * representative of their function: rangeDistanceToElevation for instance.
 */
object Algorithms {

  /**
   * Arbitrary check of the zenith angle to be within bounds
   *
   * @param zenithAngle DoubleParameter that contains zenith angle
   * @return true if valid else false
   */
  def verifyZenithAngle(zenithAngle: DoubleParameter): Boolean = zenithAngle.head < 90.0 && zenithAngle.head >= 0.0

  /**
   * Checking the input focus error against fake limits of +/- 20
   *
   * @param focusError DoubleParameter that contains focusError
   * @return true if valid else false
   */
  def verifyFocusError(calculationConfig: TromboneCalculationConfig, focusError: DoubleParameter): Boolean =
    focusError.head >= calculationConfig.lowerFocusLimit && focusError.head <= calculationConfig.upperFocusLimit

  def zenithAngleToRangeDistance(elevation: Double, zenithAngle: Double): Double =
    elevation / Math.cos(Math.toRadians(zenithAngle))

  def rangeDistanceToElevation(rangeDistance: Double, zenithAngle: Double): Double =
    Math.cos(Math.toRadians(zenithAngle)) * rangeDistance

  def focusZenithAngleToElevationAndRangeDistance(calculationConfig: TromboneCalculationConfig,
                                                  elevation: Double,
                                                  focusError: Double,
                                                  zenithAngle: Double): (Double, Double) = {
    val totalRangeDistance = focusZenithAngleToRangeDistance(calculationConfig, elevation, focusError, zenithAngle)
    val newElevation       = rangeDistanceToElevation(totalRangeDistance, zenithAngle)
    (totalRangeDistance, newElevation)
  }

  def focusZenithAngleToRangeDistance(calculationConfig: TromboneCalculationConfig,
                                      elevation: Double,
                                      focusError: Double,
                                      zenithAngle: Double): Double = {
    val rangeDistance1     = zenithAngleToRangeDistance(elevation, zenithAngle)
    val rangeError         = focusErrorToRangeError(calculationConfig, focusError)
    val totalRangeDistance = rangeDistance1 + rangeError
    totalRangeDistance
  }

  // The focus to range distance is  the gain * focus value/4 where focus is pinned to be between +/- 20
  def focusErrorToRangeError(calculationConfig: TromboneCalculationConfig, focusError: Double): Double = {
    // Limit the focus Error
    val pinnedFocusValue =
      Math.max(calculationConfig.lowerFocusLimit, Math.min(calculationConfig.upperFocusLimit, focusError))
    calculationConfig.focusErrorGain * pinnedFocusValue / 4.0
  }

  /**
   * Converts the range distance is kilometers to stage position in millimeters
   * Note that in this example, the stage position is just the elevation value
   * Note that the units must be checked in the caller
   *
   * @param rangeDistance in kilometer units
   * @return stage position in millimeters
   */
  def rangeDistanceToStagePosition(rangeDistance: Double): Double = rangeDistance

  /**
   *
   * @param stagePosition is the value of the stage position in millimeters (currently the total NA elevation)
   * @return DoubleParameter with key naTrombonePosition and units of enc
   */
  def stagePositionToEncoder(controlConfig: TromboneControlConfig, stagePosition: Double): Int = {
    // Scale value to be between 200 and 1000 encoder
    val encoderValue =
      (controlConfig.positionScale * (stagePosition - controlConfig.stageZero) + controlConfig.minStageEncoder).toInt
    val pinnedEncValue = Math.max(controlConfig.minEncoderLimit, Math.min(controlConfig.maxEncoderLimit, encoderValue))
    pinnedEncValue
  }

}
