/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.data.command

import csw.params.core.models.Units
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs

case class UnitsMap(data: Map[String, String])

object UnitsMap {
  implicit lazy val unitsMapCodec: Codec[UnitsMap] = MapBasedCodecs.deriveCodec

  lazy val value: UnitsMap = UnitsMap(Units.values.map(u => (u.entryName, u.toString)).toMap)
}
