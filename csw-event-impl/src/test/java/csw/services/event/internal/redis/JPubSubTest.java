package csw.services.event.internal.redis;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.*;
import csw.messages.params.models.Prefix;
import csw.services.event.JRedisFactory;
import csw.services.event.helpers.RegistrationFactory;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.commons.EventServiceConnection;
import csw.services.event.internal.commons.Wiring;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.IEventSubscription;
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

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class JPubSubTest {
    private static int seedPort = 3562;
    private static int redisPort = 6379;

    private static ClusterSettings clusterSettings;
    private static RedisServer redis;
    private static RedisClient redisClient;
    private static Wiring wiring;
    private static IEventPublisher publisher;
    private static IEventSubscriber subscriber;

    private int counter = -1;
    private Cancellable cancellable;

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
        subscriber = redisFactory.subscriber().get();

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
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(set).toMat(Sink.seq(), Keep.both()).run(wiring.resumingMat());
        Thread.sleep(2000);

        publisher.publish(event1).get();
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent());
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get());
    }

    @Test
    public void shouldAbleToMakeIndependentSubscriptions() throws InterruptedException, ExecutionException, TimeoutException {
        Prefix prefix = new Prefix("test.prefix");
        EventName eventName1 = new EventName("system1");
        EventName eventName2 = new EventName("system2");
        Event event1 = new SystemEvent(prefix, eventName1);
        Event event2 = new SystemEvent(prefix, eventName2);

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(event1.eventKey());

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(set).toMat(Sink.seq(), Keep.both()).run(wiring.resumingMat());
        Thread.sleep(1000);
        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        java.util.Set<EventKey> set2 = new HashSet<>();
        set2.add(event2.eventKey());

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair2 = subscriber.subscribe(set2).toMat(Sink.seq(), Keep.both()).run(wiring.resumingMat());
        Thread.sleep(1000);
        publisher.publish(event2).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);
        pair2.first().unsubscribe().get(10, TimeUnit.SECONDS);

        List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent());
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get());

        List<Event> expectedEvents2 = new ArrayList<>();
        expectedEvents2.add(Event$.MODULE$.invalidEvent());
        expectedEvents2.add(event2);
        Assert.assertEquals(expectedEvents2, pair2.second().toCompletableFuture().get());
    }

    @Test
    public void shouldBeAbleToPublishConcurrentlyToTheSameChannel() throws InterruptedException {
        java.util.List<Event> events = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            events.add(Utils.makeEvent(i));
        }

        Set<EventKey> eventKeys = new HashSet<>();
        eventKeys.add(Utils.makeEvent(0).eventKey());

        List<Event> queue = new ArrayList<>();
        subscriber.subscribe(eventKeys).runForeach(queue::add, wiring.resumingMat());

        Thread.sleep(10);

        cancellable = publisher.publish(() -> {
            counter += 1;
            if (counter == 10) cancellable.cancel();
            return events.get(counter);
        }, new FiniteDuration(2, TimeUnit.MILLISECONDS));


        Thread.sleep(1000); //TODO : Try to replace with Await

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(11, queue.size());

        events.add(0, Event$.MODULE$.invalidEvent());
        Assert.assertEquals(events, queue);
    }

    @Test
    public void shouldBeAbleToPublishMultipleToDifferentChannels() throws InterruptedException {
        java.util.List<Event> events = new ArrayList<>();
        for (int i = 101; i < 111; i++) {
            events.add(Utils.makeDistinctEvent(i));
        }

        List<Event> queue = new ArrayList<>();
        subscriber.subscribe(events.stream().map(Event::eventKey).collect(Collectors.toSet())).runForeach(queue::add, wiring.resumingMat());

        Thread.sleep(500);

        publisher.publish(Source.fromIterator(events::iterator));

        Thread.sleep(1000);

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(11, queue.size());

        events.add(0, Event$.MODULE$.invalidEvent());
        Assert.assertEquals(events, queue);
    }

    @Test
    public void shouldBeAbleToRetrieveRecentlyPublishedEventOnSubscription() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        Event event2 = Utils.makeEvent(2);
        Event event3 = Utils.makeEvent(3);
        EventKey eventKey = event1.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        publisher.publish(event2).get(10, TimeUnit.SECONDS);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(eventKey)).toMat(Sink.seq(), Keep.both()).run(wiring.resumingMat());
        Thread.sleep(1000);

        publisher.publish(event3).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        java.util.List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(event2);
        expectedEvents.add(event3);

        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldBeAbleToRetrieveInvalidEvent() throws InterruptedException, TimeoutException, ExecutionException {
        EventKey eventKey = new EventKey("test");

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(eventKey)).toMat(Sink.seq(), Keep.both()).run(wiring.resumingMat());
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        Assert.assertEquals(Collections.singletonList(Event$.MODULE$.invalidEvent()), pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldBeAbleToGetAnEventWithoutSubscribingForIt() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        EventKey eventKey = event1.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        Event event = subscriber.get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(event1, event);
    }

    @Test
    public void shouldBeAbleToRetrieveInvalidEventOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = new EventKey("test");
        Event event = subscriber.get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(Event$.MODULE$.invalidEvent(), event);
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
