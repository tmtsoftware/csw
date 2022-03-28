/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.models

sealed abstract class NetworkType(val envKey: String)

object NetworkType {
  case object Outside extends NetworkType("AAS_INTERFACE_NAME")
  case object Inside  extends NetworkType("INTERFACE_NAME")
}
