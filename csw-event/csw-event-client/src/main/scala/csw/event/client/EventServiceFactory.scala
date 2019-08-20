package csw.event.client

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.stream.{ActorMaterializerSettings, Materializer}
import csw.event.api.javadsl.IEventService
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.EventStreamSupervisionStrategy
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.event.client.internal.commons.serviceresolver.{
  EventServiceHostPortResolver,
  EventServiceLocationResolver,
  EventServiceResolver
}
import csw.event.client.internal.kafka.KafkaEventService
import csw.event.client.internal.redis.RedisEventService
import csw.event.client.models.EventStore
import csw.event.client.models.EventStores.{KafkaStore, RedisStore}
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService

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
   * API to create [[csw.event.api.scaladsl.EventService]] using [[csw.location.api.scaladsl.LocationService]] to resolve Event Server.
   *
   * @param locationService instance of location service
   * @param system an actor system required for underlying event streams
   * @return [[csw.event.api.scaladsl.EventService]] which provides handles to [[csw.event.api.scaladsl.EventPublisher]] and [[csw.event.api.scaladsl.EventSubscriber]]
   */
  def make(locationService: LocationService)(implicit system: ActorSystem[_]): EventService =
    eventService(new EventServiceLocationResolver(locationService)(system.executionContext))

  /**
   * API to create [[csw.event.api.scaladsl.EventService]] using host and port of Event Server.
   *
   * @param host hostname of event server
   * @param port port on which event server is running
   * @param system an actor system required for underlying event streams
   * @return [[csw.event.api.scaladsl.EventService]] which provides handles to [[csw.event.api.scaladsl.EventPublisher]] and [[csw.event.api.scaladsl.EventSubscriber]]
   */
  def make(host: String, port: Int)(implicit system: ActorSystem[_]): EventService =
    eventService(new EventServiceHostPortResolver(host, port))

  /**
   * Java API to create [[csw.event.api.javadsl.IEventService]] using [[csw.location.api.javadsl.ILocationService]] to resolve Event Server.
   *
   * @param locationService instance of location service
   * @param actorSystem an actor system required for underlying event streams
   * @return [[csw.event.api.javadsl.IEventService]] which provides handles to [[csw.event.api.javadsl.IEventPublisher]] and [[csw.event.api.javadsl.IEventSubscriber]]
   */
  def jMake(locationService: ILocationService, actorSystem: ActorSystem[_]): IEventService = {
    val eventService = make(locationService.asScala)(actorSystem)
    new JEventService(eventService)
  }

  /**
   * Java API to create [[csw.event.api.javadsl.IEventService]] using host and port of Event Server.
   *
   * @param host hostname of event server
   * @param port port on which event server is running
   * @param system an actor system required for underlying event streams
   * @return [[csw.event.api.javadsl.IEventService]] which provides handles to [[csw.event.api.javadsl.IEventPublisher]] and [[csw.event.api.javadsl.IEventSubscriber]]
   */
  def jMake(host: String, port: Int, system: ActorSystem[_]): IEventService = {
    val eventService = make(host, port)(system)
    new JEventService(eventService)
  }

  private def mat()(implicit actorSystem: ActorSystem[_]): Materializer =
    ActorMaterializer(
      Some(ActorMaterializerSettings(actorSystem.toUntyped).withSupervisionStrategy(EventStreamSupervisionStrategy.decider))
    )

  private def eventService(eventServiceResolver: EventServiceResolver)(implicit system: ActorSystem[_]) = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val materializer: Materializer = mat()

    def masterId = system.settings.config.getString("csw-event.redis.masterId")

    store match {
      case RedisStore(client) => new RedisEventService(eventServiceResolver, masterId, client)
      case KafkaStore         => new KafkaEventService(eventServiceResolver)
    }
  }
}
