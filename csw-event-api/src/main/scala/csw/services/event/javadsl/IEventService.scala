package csw.services.event.javadsl

trait IEventService {
  val defaultPublisher: IEventPublisher
  val defaultSubscriber: IEventSubscriber

  def makeNewPublisher(): IEventPublisher

}
