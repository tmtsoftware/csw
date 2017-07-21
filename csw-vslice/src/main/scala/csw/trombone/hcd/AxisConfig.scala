package csw.trombone.hcd

import com.typesafe.config.Config

/**
 * Axis configuration
 */
object AxisConfig {
  def apply(config: Config): AxisConfig = {
    // Main prefix for keys used below
    val prefix = "csw.examples.trombone.hcd"

    val axisName: String   = config.getString(s"$prefix.axis-config.axisName")
    val lowLimit: Int      = config.getInt(s"$prefix.axis-config.lowLimit")
    val lowUser: Int       = config.getInt(s"$prefix.axis-config.lowUser")
    val highUser: Int      = config.getInt(s"$prefix.axis-config.highUser")
    val highLimit: Int     = config.getInt(s"$prefix.axis-config.highLimit")
    val home: Int          = config.getInt(s"$prefix.axis-config.home")
    val startPosition: Int = config.getInt(s"$prefix.axis-config.startPosition")
    val stepDelayMS: Int   = config.getInt(s"$prefix.axis-config.stepDelayMS")
    AxisConfig(axisName, lowLimit, lowUser, highUser, highLimit, home, startPosition, stepDelayMS)
  }
}

/**
 * Axis configuration
 */
case class AxisConfig(axisName: String,
                      lowLimit: Int,
                      lowUser: Int,
                      highUser: Int,
                      highLimit: Int,
                      home: Int,
                      startPosition: Int,
                      stepDelayMS: Int)
