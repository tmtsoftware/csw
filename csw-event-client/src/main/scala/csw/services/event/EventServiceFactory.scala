package csw.services.event

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import csw.services.location.api.javadsl.ILocationService
import csw.services.location.api.scaladsl.LocationService
import csw.services.event.api.javadsl.IEventService
import csw.services.event.api.scaladsl.EventService
import csw.services.event.internal.commons.EventStreamSupervisionStrategy
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.internal.commons.serviceresolver.{
  EventServiceHostPortResolver,
  EventServiceLocationResolver,
  EventServiceResolver
}
import csw.services.event.internal.kafka.KafkaEventService
import csw.services.event.internal.redis.RedisEventService
import csw.services.event.models.EventStore
import csw.services.event.models.EventStores.{KafkaStore, RedisStore}

import scala.concurrent.ExecutionContext

/**
 * Factory to create EventService
 */
class EventServiceFactory(store: EventStore = RedisStore()) {

  /**
   * A java helper to construct EventServiceFactory
   */
  def this() = this(RedisStore())

  /**
   * API to create [[EventService]] using [[LocationService]] to resolve Event Server.
   * @param locationService instance of location service
   * @param system an actor system required for underlying event streams
   * @return [[EventService]] which provides handles to [[csw.services.event.api.scaladsl.EventPublisher]] and [[csw.services.event.api.scaladsl.EventSubscriber]]
   */
  def make(locationService: LocationService)(implicit system: ActorSystem): EventService =
    eventService(new EventServiceLocationResolver(locationService)(system.dispatcher))

  /**
   * API to create [[EventService]] using host and port of Event Server.
   * @param host hostname of event server
   * @param port port on which event server is running
   * @param system an actor system required for underlying event streams
   * @return [[EventService]] which provides handles to [[csw.services.event.api.scaladsl.EventPublisher]] and [[csw.services.event.api.scaladsl.EventSubscriber]]
   */
  def make(host: String, port: Int)(implicit system: ActorSystem): EventService =
    eventService(new EventServiceHostPortResolver(host, port))

  /**
   * Java API to create [[IEventService]] using [[ILocationService]] to resolve Event Server.
   * @param locationService instance of location service
   * @param actorSystem an actor system required for underlying event streams
   * @return [[IEventService]] which provides handles to [[csw.services.event.api.javadsl.IEventPublisher]] and [[csw.services.event.api.javadsl.IEventSubscriber]]
   */
  def jMake(locationService: ILocationService, actorSystem: ActorSystem): IEventService = {
    val eventService = make(locationService.asScala)(actorSystem)
    new JEventService(eventService)
  }

  /**
   * Java API to create [[IEventService]] using host and port of Event Server.
   * @param host hostname of event server
   * @param port port on which event server is running
   * @param system an actor system required for underlying event streams
   * @return [[IEventService]] which provides handles to [[csw.services.event.api.javadsl.IEventPublisher]] and [[csw.services.event.api.javadsl.IEventSubscriber]]
   */
  def jMake(host: String, port: Int, system: ActorSystem): IEventService = {
    val eventService = make(host, port)(system)
    new JEventService(eventService)
  }

  private def mat()(implicit actorSystem: ActorSystem): Materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(EventStreamSupervisionStrategy.decider))

  private def eventService(eventServiceResolver: EventServiceResolver)(implicit system: ActorSystem) = {
    implicit val ec: ExecutionContext       = system.dispatcher
    implicit val materializer: Materializer = mat()

    def masterId = system.settings.config.getString("csw-event.redis.masterId")

    store match {
      case RedisStore(client) ⇒ new RedisEventService(eventServiceResolver, masterId, client)
      case KafkaStore         ⇒ new KafkaEventService(eventServiceResolver)
    }
  }
}
