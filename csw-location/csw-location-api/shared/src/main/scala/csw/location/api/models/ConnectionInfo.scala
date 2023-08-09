/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.models

import csw.prefix.models.Prefix

/**
 * ConnectionInfo represents a component name, component type and connection type
 *
 * @param prefix represents a prefix of a component e.g. nfiraos.TromboneAssembly
 * @param componentType represents the type of component e.g. Assembly, HCD, etc
 * @param connectionType represents the type of connection e.g. pekko, http, tcp
 */
case class ConnectionInfo(prefix: Prefix, componentType: ComponentType, connectionType: ConnectionType) {
  override def toString: String = s"$prefix-${componentType.name}-${connectionType.name}"
}
