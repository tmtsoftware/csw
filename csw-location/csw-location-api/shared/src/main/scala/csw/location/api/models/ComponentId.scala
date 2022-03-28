/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.models

import csw.prefix.models.Prefix

/**
 * Represents a component based on its prefix and type.
 *
 * @note Prefix should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 *  @param prefix represents the prefix (subsystem and name) of the component e.g. tcs.filter.wheel
 *  @param componentType represents a type of the Component e.g. Assembly, HCD, Sequencer etc
 */
case class ComponentId(prefix: Prefix, componentType: ComponentType) {

  /**
   * Represents the name and componentType
   */
  def fullName: String = s"$prefix-${componentType.name}"
}
