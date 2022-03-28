/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons

import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.internal.commons.javawrappers.{JEventPublisher, JEventService, JEventSubscriber}

/**
 * Adapt scala APIs of Event Service to java APIs
 */
object EventServiceExt {

  implicit class RichEventPublisher(val eventPublisher: EventPublisher) {
    def asJava: IEventPublisher = new JEventPublisher(eventPublisher)
  }

  implicit class RichEventSubscriber(val eventSubscriber: EventSubscriber) {
    def asJava: IEventSubscriber = new JEventSubscriber(eventSubscriber)
  }

  implicit class RichEventService(val eventService: EventService) {
    def asJava: IEventService = new JEventService(eventService)
  }
}
