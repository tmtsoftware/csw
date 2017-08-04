package csw.param.parameters

import csw.param.Subsystem
import csw.param.models.Prefix

/**
 * A trait to be mixed in that provides a parameter set and prefix info
 */
trait ParameterSetKeyData { self: ParameterSetType[_] =>

  /**
   * Returns an object providing the subsystem and prefix for the parameter set
   */
  def prefix: Prefix

  /**
   * The subsystem for the parameter set
   */
  final def subsystem: Subsystem = prefix.subsystem

  /**
   * The prefix for the parameter set
   */
  final def prefixStr: String = prefix.prefix

  // This is the get for a Setup/Observe/Wait
  override def toString = s"$typeName([$subsystem, $prefixStr]$dataToString)"
}
