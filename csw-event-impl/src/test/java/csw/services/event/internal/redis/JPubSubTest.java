package csw.services.event.internal.redis;

import akka.actor.ActorSystem;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.Event;
import csw.messages.events.EventKey;
import csw.services.event.helpers.RegistrationFactory;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.JEventServicePubSubTestFramework;
import csw.services.event.internal.commons.EventServiceConnection;
import csw.services.event.internal.commons.Wiring;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.JRedisFactory;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.commons.ClusterSettings;
import csw.services.location.models.TcpRegistration;
import csw.services.location.scaladsl.LocationService;
import csw.services.location.scaladsl.LocationServiceFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.embedded.RedisServer;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
public class JPubSubTest {
    private static int seedPort = 3562;
    private static int redisPort = 6379;

    private static ClusterSettings clusterSettings;
    private static RedisServer redis;
    private static RedisClient redisClient;
    private static Wiring wiring;
    private static IEventPublisher publisher;
    private static JEventServicePubSubTestFramework framework;

    @BeforeClass
    public static void beforeClass() throws Exception {
        clusterSettings = ClusterAwareSettings.joinLocal(seedPort);
        redis = RedisServer.builder().setting("bind " + clusterSettings.hostname()).port(redisPort).build();

        TcpRegistration tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value(), redisPort);
        LocationService locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort));
        Await.result(locationService.register(tcpRegistration), new FiniteDuration(10, TimeUnit.SECONDS));

        redisClient = RedisClient.create();
        ActorSystem actorSystem = clusterSettings.system();
        wiring = new Wiring(actorSystem);
        JRedisFactory redisFactory = new JRedisFactory(redisClient, locationService, wiring);

        publisher = redisFactory.publisher().get();
        IEventSubscriber subscriber = redisFactory.subscriber().get();

        framework = new JEventServicePubSubTestFramework(publisher, subscriber, wiring.resumingMat());

        redis.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisClient.shutdown();
        redis.stop();
        Await.result(wiring.shutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$), new FiniteDuration(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldBeAbleToPublishAndSubscribeAnEvent() throws InterruptedException, TimeoutException, ExecutionException {
       framework.pubsub();
    }

    @Test
    public void shouldAbleToMakeIndependentSubscriptions() throws InterruptedException, ExecutionException, TimeoutException {
        framework.subscribeIndependently();
    }

    @Test
    public void shouldBeAbleToPublishConcurrentlyToTheSameChannel() throws InterruptedException {
        framework.publishMultiple();
    }

    @Test
    public void shouldBeAbleToPublishMultipleToDifferentChannels() throws InterruptedException {
        framework.publishMultipleToDifferentChannels();
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test
    public void shouldBeAbleToRetrieveRecentlyPublishedEventOnSubscription() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveRecentlyPublished();
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test
    public void shouldBeAbleToRetrieveInvalidEvent() throws InterruptedException, TimeoutException, ExecutionException {
        framework.retrieveInvalidEvent();
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test
    public void shouldBeAbleToRetrieveOnlyValidEventsWhenOneOfTheSubscribedEventsKeysHasPublishedEvents() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveMultipleSubscribedEvents();
    }

    @Test
    public void shouldBeAbleToGetAnEventWithoutSubscribingForIt() throws InterruptedException, ExecutionException, TimeoutException {
        framework.get();
    }

    @Test
    public void shouldBeAbleToRetrieveInvalidEventOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveInvalidEventOnGet();
    }

    @Test
    public void shouldBeAbleToPersistEventInDBWhilePublishing() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        EventKey eventKey = event1.eventKey();
        RedisCommands<EventKey, Event> redisCommands =
                redisClient.connect(EventServiceCodec$.MODULE$, RedisURI.create(clusterSettings.hostname(), redisPort)).sync();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        Thread.sleep(1000);
        Assert.assertEquals(event1, redisCommands.get(eventKey));
    }
}
