/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.models

object Separator {
  val Hyphen: String = "-"

  def hyphenate(ins: String*): String = ins.mkString(Hyphen)
}
