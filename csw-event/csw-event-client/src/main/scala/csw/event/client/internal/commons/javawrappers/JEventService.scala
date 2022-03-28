/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons.javawrappers

import csw.event.api.javadsl.IEventService
import csw.event.api.scaladsl.EventService

/**
 * Java API for [[csw.event.api.scaladsl.EventService]]
 */
class JEventService(eventService: EventService) extends IEventService {

  override def makeNewPublisher(): JEventPublisher = new JEventPublisher(eventService.makeNewPublisher())

  override def makeNewSubscriber(): JEventSubscriber = new JEventSubscriber(eventService.defaultSubscriber)

  override def asScala: EventService = eventService
}
