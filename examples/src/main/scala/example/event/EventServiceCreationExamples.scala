package example.event

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.{KafkaStore, RedisStore}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import io.lettuce.core.ClientOptions.DisconnectedBehavior
import io.lettuce.core.{ClientOptions, RedisClient}

class EventServiceCreationExamples {

  private implicit val actorSystem: ActorSystem = ActorSystemFactory.remote()
  private implicit val mat: ActorMaterializer   = ActorMaterializer()
  private val locationService                   = HttpLocationServiceFactory.makeLocalClient

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

    val clientOptions = ClientOptions.builder().disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS).build
    val redisClient   = RedisClient.create()
    redisClient.setOptions(clientOptions)

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
