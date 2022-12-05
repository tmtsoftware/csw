/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.generics

import csw.prefix.models.{Prefix, Subsystem}

/**
 * A trait to be mixed in that provides a parameter set and prefix info
 */
trait ParameterSetKeyData extends ParameterSetType[ParameterSetKeyData] {

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
  final def prefixStr: String = prefix.toString

  /**
   * A String representation for concrete implementation of this trait
   */
  override def toString: String = s"$typeName([$prefix]$dataToString)"
}
