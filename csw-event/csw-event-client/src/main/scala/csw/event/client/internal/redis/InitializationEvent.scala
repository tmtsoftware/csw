/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.redis
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventKey, SystemEvent}
import csw.prefix.models.Subsystem

private[event] object InitializationEvent {

  private val initParam = StringKey.make("InitKey").set("IGNORE: Redis publisher initialization")
  private val prefix    = s"${Subsystem.CSW}.first.event"
  private val eventKey  = EventKey(s"$prefix.init")

  def value: SystemEvent = SystemEvent(eventKey.source, eventKey.eventName, Set(initParam))

}
