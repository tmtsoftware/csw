package csw.services.event

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
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
import csw.services.event.models.EventStore.{KafkaStore, RedisStore}
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService

import scala.concurrent.ExecutionContext

/**
 * Factory to create EventService
 */
class EventServiceFactory(store: EventStore = RedisStore()) {

  def make(locationService: LocationService)(implicit system: ActorSystem): EventService =
    eventService(new EventServiceLocationResolver(locationService)(system.dispatcher))

  def make(host: String, port: Int)(implicit system: ActorSystem): EventService =
    eventService(new EventServiceHostPortResolver(host, port))

  def jMake(locationService: ILocationService, actorSystem: ActorSystem): IEventService = {
    val eventService = make(locationService.asScala)(actorSystem)
    new JEventService(eventService)
  }

  def jMake(host: String, port: Int, actorSystem: ActorSystem): IEventService = {
    val eventService = make(host, port)(actorSystem)
    new JEventService(eventService)
  }

  private def mat()(implicit actorSystem: ActorSystem): Materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(EventStreamSupervisionStrategy.decider))

  private def eventService(eventServiceResolver: EventServiceResolver)(implicit system: ActorSystem) = {
    implicit val ec: ExecutionContext       = system.dispatcher
    implicit val materializer: Materializer = mat()

    def masterId = system.settings.config.getString("redis.masterId")

    store match {
      case RedisStore(client) ⇒ new RedisEventService(eventServiceResolver, masterId, client)
      case KafkaStore         ⇒ new KafkaEventService(eventServiceResolver)
    }
  }
}
