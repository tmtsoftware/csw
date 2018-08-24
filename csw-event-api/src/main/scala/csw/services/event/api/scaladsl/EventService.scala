package csw.services.event.api.scaladsl

import scala.concurrent.ExecutionContext

/**
 * An interface to provide access to [[csw.services.event.api.scaladsl.EventPublisher]] and [[csw.services.event.api.scaladsl.EventSubscriber]].
 */
trait EventService {
  implicit val executionContext: ExecutionContext

  /**
   * A default instance of [[csw.services.event.api.scaladsl.EventPublisher]].
   * This could be shared across under normal operating conditions to share the underlying connection to event server.
   */
  lazy val defaultPublisher: EventPublisher = makeNewPublisher()

  /**
   * A default instance of [[csw.services.event.api.scaladsl.EventSubscriber]].
   * This could be shared across under normal operating conditions to share the underlying connection to event server.
   */
  lazy val defaultSubscriber: EventSubscriber = makeNewSubscriber()

  /**
   * Create a new instance of [[csw.services.event.api.scaladsl.EventPublisher]] with a separate underlying connection than the default instance.
   * The new instance will be required when the location of Event Service is updated or in case the performance requirements
   * of a publish operation demands a separate connection to be used.
   * @return new instance of [[csw.services.event.api.scaladsl.EventPublisher]]
   */
  def makeNewPublisher(): EventPublisher

  /**
   * Create a new instance of [[csw.services.event.api.scaladsl.EventPublisher]] with a separate underlying connection than the default instance.
   * The new instance will be required when the location of Event Service is updated or in case the performance requirements
   * of a subscribe operation demands a separate connection to be used.
   * @return new instance of [[csw.services.event.api.scaladsl.EventSubscriber]]
   */
  def makeNewSubscriber(): EventSubscriber
}
