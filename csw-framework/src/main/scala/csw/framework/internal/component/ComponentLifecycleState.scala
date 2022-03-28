/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.component

/**
 * Lifecycle state of a Component TLA actor
 */
private[framework] sealed trait ComponentLifecycleState
private[framework] object ComponentLifecycleState {
  case object Idle        extends ComponentLifecycleState
  case object Initialized extends ComponentLifecycleState
  case object Running     extends ComponentLifecycleState
}
