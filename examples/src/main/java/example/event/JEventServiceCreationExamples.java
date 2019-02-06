package example.event;

import akka.actor.ActorSystem;
import csw.event.api.javadsl.IEventService;
import csw.event.client.EventServiceFactory;
import csw.event.client.models.EventStores;
import csw.location.api.javadsl.ILocationService;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;

public class JEventServiceCreationExamples {

    private ActorSystem actorSystem;
    private ILocationService locationService;

    public JEventServiceCreationExamples(ActorSystem actorSystem, ILocationService locationService) {
        this.actorSystem = actorSystem;
        this.locationService = locationService;
    }

    private void createDefaultEventService() {
        //#default-event-service
        // create event service using host and port of event server.
        IEventService eventService1 = new EventServiceFactory().jMake(locationService, actorSystem);

        // create event service using host and port of event server.
        IEventService eventService2 = new EventServiceFactory().jMake("localhost", 26379, actorSystem);
        //#default-event-service
    }

    private void createRedisEventService() {

        //#redis-event-service
        ClientOptions clientOptions = ClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build();
        RedisClient redisClient   = RedisClient.create();
        redisClient.setOptions(clientOptions);

        EventStores.RedisStore redisStore = new EventStores.RedisStore(redisClient);
        // create event service using location service
        IEventService eventService1 = new EventServiceFactory(redisStore).jMake(locationService, actorSystem);

        // create event service using host and port of event server.
        IEventService eventService2 = new EventServiceFactory(redisStore).jMake("localhost", 26379, actorSystem);
        //#redis-event-service
    }

    private void createKafkaEventService() {

        //#kafka-event-service
        // create event service using location service
        IEventService eventService1 = new EventServiceFactory(EventStores.jKafkaStore()).jMake(locationService, actorSystem);

        // create event service using host and port of event server.
        IEventService eventService2 = new EventServiceFactory(EventStores.jKafkaStore()).jMake("localhost", 26379, actorSystem);
        //#kafka-event-service
    }
}
