/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.events

import csw.params.core.models.Choice
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class CoordinateSystem extends EnumEntry

/**
 *  coordinate system enumeration.
 *  RADEC, XY, ALTAZ.
 *  RADEC indicates values will be in accordance of Right Ascension & Declination.
 *  XY indicates values will be in accordance of cartesian coordinate system.
 *  ALTAZ indicates values will be in accordance of altitude & azimuth.
 *  This could  happen as a result of a command or a spontaneous failure.
 *  Corrective  action is required.
 */
object CoordinateSystem extends Enum[CoordinateSystem] {
  override def values: immutable.IndexedSeq[CoordinateSystem] = findValues

  case object RADEC extends CoordinateSystem

  case object XY extends CoordinateSystem

  case object ALTAZ extends CoordinateSystem

  def toChoices: Seq[Choice] = CoordinateSystem.values.map(x => Choice(x.entryName))
}
