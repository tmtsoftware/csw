/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.events

/**
 * A wrapper class representing the name of an Event
 */
case class EventName(name: String) {
  override def toString: String = name
}
