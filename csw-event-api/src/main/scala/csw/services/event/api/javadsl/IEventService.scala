package csw.services.event.api.javadsl

import csw.services.event.api.scaladsl.EventService

import scala.concurrent.ExecutionContext

/**
 * An interface to provide access to [[csw.services.event.api.javadsl.IEventPublisher]] and [[csw.services.event.api.javadsl.IEventSubscriber]].
 */
trait IEventService {

  implicit val executionContext: ExecutionContext

  /**
   * A default instance of [[csw.services.event.api.javadsl.IEventPublisher]].
   * This could be shared across under normal operating conditions to share the underlying connection to event server.
   */
  lazy val defaultPublisher: IEventPublisher = makeNewPublisher()

  /**
   * A default instance of [[csw.services.event.api.javadsl.IEventSubscriber]].
   * This could be shared across under normal operating conditions to share the underlying connection to event server.
   */
  lazy val defaultSubscriber: IEventSubscriber = makeNewSubscriber()

  /**
   * Create a new instance of [[csw.services.event.api.javadsl.IEventPublisher]] with a separate underlying connection than the default instance.
   * The new instance will be required when the location of Event Service is updated or in case the performance requirements
   * of a publish operation demands a separate connection to be used.
   * @return new instance of [[csw.services.event.api.javadsl.IEventPublisher]]
   */
  def makeNewPublisher(): IEventPublisher

  /**
   * Create a new instance of [[csw.services.event.api.javadsl.IEventPublisher]] with a separate underlying connection than the default instance.
   * The new instance will be required when the location of Event Service is updated or in case the performance requirements
   * of a subscribe operation demands a separate connection to be used.
   * @return new instance of [[csw.services.event.api.javadsl.IEventSubscriber]]
   */
  def makeNewSubscriber(): IEventSubscriber

  /**
   * Returns the Scala API for this instance of event service
   */
  def asScala: EventService
}
