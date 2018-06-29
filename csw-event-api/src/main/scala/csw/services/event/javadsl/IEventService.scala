package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import csw.services.event.scaladsl.EventService

import scala.concurrent.ExecutionContext

/**
 * An interface to provide access to [[csw.services.event.javadsl.IEventPublisher]] and [[csw.services.event.javadsl.IEventSubscriber]].
 */
trait IEventService {

  implicit val executionContext: ExecutionContext

  /**
   * A default instance of [[csw.services.event.javadsl.IEventPublisher]].
   * This could be shared across under normal operating conditions to share the underlying connection to event server.
   */
  lazy val defaultPublisher: CompletableFuture[IEventPublisher] = makeNewPublisher()

  /**
   * A default instance of [[csw.services.event.javadsl.IEventSubscriber]].
   * This could be shared across under normal operating conditions to share the underlying connection to event server.
   */
  lazy val defaultSubscriber: CompletableFuture[IEventSubscriber] = makeNewSubscriber()

  /**
   * Create a new instance of [[csw.services.event.javadsl.IEventPublisher]] with a separate underlying connection than the default instance.
   * The new instance will be required when the location of Event Service is updated or in case the performance requirements
   * of a publish operation demands a separate connection to be used.
   * @return
   */
  def makeNewPublisher(): CompletableFuture[IEventPublisher]

  /**
   * Create a new instance of [[csw.services.event.javadsl.IEventPublisher]] with a separate underlying connection than the default instance.
   * The new instance will be required when the location of Event Service is updated or in case the performance requirements
   * of a subscribe operation demands a separate connection to be used.
   * @return A new instance of [[csw.services.event.javadsl.IEventSubscriber]]
   */
  def makeNewSubscriber(): CompletableFuture[IEventSubscriber]

  /**
   * Returns the Scala API for this instance of event service
   */
  def asScala: EventService
}
