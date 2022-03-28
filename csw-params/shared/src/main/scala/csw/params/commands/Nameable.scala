/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.commands
import csw.params.core.states.StateName

trait Nameable[T] {
  def name(state: T): StateName
}
