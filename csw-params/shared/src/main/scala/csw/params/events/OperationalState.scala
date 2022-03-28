/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.events

import csw.params.core.models.Choice
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class OperationalState extends EnumEntry

/**
 * Enumeration indicating if the detector system is available and operational.
 *  READY, BUSY, ERROR.
 *  READY indicates system can execute exposures.
 *  BUSY indicates system is BUSY most likely acquiring data.
 *  ERROR indicates the detector system is in an error state.
 *  This could  happen as a result of a command or a spontaneous failure.
 *  Corrective  action is required.
 */
object OperationalState extends Enum[OperationalState] {
  override def values: immutable.IndexedSeq[OperationalState] = findValues

  case object READY extends OperationalState

  case object NOT_READY extends OperationalState

  case object ERROR extends OperationalState

  case object BUSY extends OperationalState

  def toChoices: Seq[Choice] = OperationalState.values.map(x => Choice(x.entryName))
}

object JOperationalState {
  val READY: OperationalState     = OperationalState.READY
  val ERROR: OperationalState     = OperationalState.ERROR
  val BUSY: OperationalState      = OperationalState.BUSY
  val NOT_READY: OperationalState = OperationalState.NOT_READY
}
