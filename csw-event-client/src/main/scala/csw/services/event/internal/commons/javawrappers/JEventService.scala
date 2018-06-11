package csw.services.event.internal.commons.javawrappers

import csw.services.event.internal.commons.EventServiceAdapter
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl.EventService

class JEventService(eventService: EventService) extends IEventService {

  override val defaultPublisher: IEventPublisher = EventServiceAdapter.asJava(eventService.defaultPublisher)

  override val defaultSubscriber: IEventSubscriber = EventServiceAdapter.asJava(eventService.defaultSubscriber)

  override def makeNewPublisher(): IEventPublisher = EventServiceAdapter.asJava(eventService.makeNewPublisher())
}
