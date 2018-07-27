package csw.services.event

import akka.actor.ActorSystem
import csw.services.event.api.scaladsl.EventService
import csw.services.event.models.EventStores.{KafkaStore, RedisStore}
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.scaladsl.LocationServiceFactory
import io.lettuce.core.RedisClient

class EventServiceCreationExamples {

  implicit val actorSystem: ActorSystem = ActorSystemFactory.remote()
  private val locationService           = LocationServiceFactory.withSystem(actorSystem)
  private val redisClient               = RedisClient.create()

  def defaultEventService(): Unit = {
    //#default-event-service
    // create event service using location service
    val eventService1: EventService = new EventServiceFactory().make(locationService)

    // create event service using host and port of event server.
    val eventService2: EventService = new EventServiceFactory().make("localhost", 26379)
    //#default-event-service
  }

  def redisEventService(): Unit = {
    //#redis-event-service
    // create event service using location service
    val eventService1: EventService = new EventServiceFactory(RedisStore(redisClient)).make(locationService)

    // create event service using host and port of event server.
    val eventService2: EventService = new EventServiceFactory(RedisStore(redisClient)).make("localhost", 26379)
    //#redis-event-service
  }

  def kafkaEventService(): Unit = {
    //#kafka-event-service
    // create event service using location service
    val eventService1: EventService = new EventServiceFactory(KafkaStore).make(locationService)

    // create event service using host and port of event server.
    val eventService2: EventService = new EventServiceFactory(KafkaStore).make("localhost", 26379)
    //#kafka-event-service
  }

}
