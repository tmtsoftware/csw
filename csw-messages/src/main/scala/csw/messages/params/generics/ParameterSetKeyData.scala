package csw.messages.params.generics

import csw.messages.params.models.{Prefix, Subsystem}

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

  /**
   * A String representation for concrete implementation of this trait
   */
  override def toString: String = s"$typeName([$subsystem, $prefixStr]$dataToString)"
}
