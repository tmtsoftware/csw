package csw.params.core.models

/**
 * Java API for creating an Angle instance.
 */
object JAngle {
  import Angle._

  /**
   * Creates an Angle instance from the given value in degrees
   */
  def degree(d: Double): Angle = d.degree

  /**
   * Creates an Angle instance from the given value in arcMinutes
   */
  def arcMinute(d: Double): Angle = d.arcMinute

  /**
   * Creates an Angle instance from the given value in arcSecs
   */
  def arcSec(d: Double): Angle = d.arcSec

  /**
   * Creates an Angle instance from the given value in arcHours
   */
  def arcHour(d: Double): Angle = d.arcHour

  /**
   * Creates an Angle instance from the given value in radians
   */
  def radian(d: Double): Angle = d.radian

  /**
   * Creates an Angle instance from the given value in mas (milliarcseconds)
   */
  def mas(d: Double): Angle = d.mas
}
