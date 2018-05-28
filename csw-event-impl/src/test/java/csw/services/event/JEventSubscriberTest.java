package csw.services.event;

import akka.actor.typed.javadsl.Adapter;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.testkit.typed.javadsl.TestInbox;
import akka.testkit.typed.javadsl.TestProbe;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.*;
import csw.messages.params.models.Prefix;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.kafka.KafkaTestProps;
import csw.services.event.internal.redis.RedisTestProps;
import csw.services.event.internal.wiring.BaseProperties;
import csw.services.event.javadsl.IEventSubscription;
import io.lettuce.core.ClientOptions;
import net.manub.embeddedkafka.EmbeddedKafka$;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
public class JEventSubscriberTest extends TestNGSuite {

    private RedisTestProps redisTestProps;
    private KafkaTestProps kafkaTestProps;

    @BeforeSuite
    public void beforeAll() {
        redisTestProps = RedisTestProps.createRedisProperties(4564, 27382, 7382, ClientOptions.create());
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties(4565, 7003, Collections.EMPTY_MAP);
        redisTestProps.redisSentinel().start();
        redisTestProps.redis().start();
        EmbeddedKafka$.MODULE$.start(kafkaTestProps.config());
    }

    @AfterSuite
    public void afterAll() throws InterruptedException, ExecutionException, TimeoutException {
        redisTestProps.redisClient().shutdown();
        redisTestProps.redis().stop();
        redisTestProps.redisSentinel().stop();
        redisTestProps.wiring().jShutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$).get(10, TimeUnit.SECONDS);

        kafkaTestProps.publisher().asJava().shutdown().get(10, TimeUnit.SECONDS);
        EmbeddedKafka$.MODULE$.stop();
        kafkaTestProps.wiring().jShutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$).get(10, TimeUnit.SECONDS);
    }

    @DataProvider(name = "event-service-provider")
    public Object[] pubsubProvider() {
        return new Object[]{redisTestProps, kafkaTestProps};
    }

    //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
    //DEOPSCSW-343: Unsubscribe based on prefix and event name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_and_subscribe_an_event(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        TestProbe probe = TestProbe.create(Adapter.toTyped(baseProperties.wiring().actorSystem()));

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(set).take(2).toMat(Sink.foreach(event -> probe.ref().tell(event)), Keep.left()).run(baseProperties.wiring().resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);

        probe.expectMessage(Event$.MODULE$.invalidEvent(eventKey));
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_and_subscribe_an_event_with_duration(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        List<Event> queue = new ArrayList<>();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);

        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(set, Duration.ZERO.plusMillis(300)).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.wiring().resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        Assert.assertEquals(queue.size(), 3);
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_async_callback(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(304);

        TestProbe probe = TestProbe.create(Adapter.toTyped(baseProperties.wiring().actorSystem()));

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeAsync(Collections.singleton(event1.eventKey()), event -> {
            probe.ref().tell(event);
            return CompletableFuture.completedFuture(event);
        });
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));

    }

    //DEOPSCSW-338: Provide callback for Event alerts
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_async_callback_with_duration(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(304);
        List<Event> queue = new ArrayList<>();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);

        IEventSubscription subscription = baseProperties.jSubscriber().subscribeAsync(Collections.singleton(event1.eventKey()), event -> {
            queue.add(event);
            return CompletableFuture.completedFuture(event);
        }, Duration.ofMillis(300));

        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        Assert.assertEquals(queue.size(), 3);
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_callback(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(303);

        TestProbe probe = TestProbe.create(Adapter.toTyped(baseProperties.wiring().actorSystem()));

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeCallback(Collections.singleton(event1.eventKey()), event -> probe.ref().tell(event));
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_callback_with_duration(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(303);

        List<Event> queue = new ArrayList<>();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeCallback(Collections.singleton(event1.eventKey()), queue::add, Duration.ofMillis(300));
        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        Assert.assertEquals(queue.size(), 3);
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_an_ActorRef(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(305);

        TestProbe probe = TestProbe.create(Adapter.toTyped(baseProperties.wiring().actorSystem()));

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Collections.singleton(event1.eventKey()), probe.ref(), baseProperties.wiring().resumingMat());
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_an_ActorRef_with_duration(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(305);

        TestInbox<Event> inbox = TestInbox.create();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Collections.singleton(event1.eventKey()), inbox.getRef(), Duration.ofMillis(300));
        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        Assert.assertEquals(inbox.getAllReceived().size(), 3);
    }

    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_make_independent_subscriptions(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Prefix prefix = new Prefix("test.prefix");
        EventName eventName1 = new EventName("system1");
        EventName eventName2 = new EventName("system2");
        Event event1 = new SystemEvent(prefix, eventName1);
        Event event2 = new SystemEvent(prefix, eventName2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Collections.singleton(event1.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.wiring().resumingMat());
        pair.first().ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(100);
        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair2 = baseProperties.jSubscriber().subscribe(Collections.singleton(event2.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.wiring().resumingMat());
        pair2.first().ready().get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(event2).get(10, TimeUnit.SECONDS);

        Set<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(event1.eventKey()));
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, new HashSet<>(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS)));

        Set<Event> expectedEvents2 = new HashSet<>();
        expectedEvents2.add(Event$.MODULE$.invalidEvent(event2.eventKey()));
        expectedEvents2.add(event2);

        Assert.assertEquals(expectedEvents2, new HashSet<>(pair2.second().toCompletableFuture().get(10, TimeUnit.SECONDS)));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_recently_published_event_on_subscription(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        Event event2 = Utils.makeEvent(2);
        Event event3 = Utils.makeEvent(3);
        EventKey eventKey = event1.eventKey();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(event2).get(10, TimeUnit.SECONDS); // latest event before subscribing
        Thread.sleep(500); // Needed for redis set which is fire and forget operation

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.wiring().resumingMat());
        pair.first().ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        baseProperties.jPublisher().publish(event3).get(10, TimeUnit.SECONDS);
        java.util.List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(event2);
        expectedEvents.add(event3);

        // assertion against a list ensures that the latest event before subscribing arrives earlier in the stream
        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_InvalidEvent(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = EventKey.apply(Prefix.apply("test"), EventName.apply("test"));

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).take(1).toMat(Sink.seq(), Keep.both()).run(baseProperties.wiring().resumingMat());

        Assert.assertEquals(Collections.singletonList(Event$.MODULE$.invalidEvent(eventKey)), pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_valid_as_well_as_invalid_event_when_events_are_published_for_some_and_not_for_other_keys(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event distinctEvent1 = Utils.makeDistinctEvent(301);
        Event distinctEvent2 = Utils.makeDistinctEvent(302);

        EventKey eventKey1 = distinctEvent1.eventKey();
        EventKey eventKey2 = distinctEvent2.eventKey();

        baseProperties.jPublisher().publish(distinctEvent1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        Set<EventKey> eventKeys = new HashSet<>();
        eventKeys.add(eventKey1);
        eventKeys.add(eventKey2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(eventKeys).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.wiring().resumingMat());

        Set<Event> actualEvents = new HashSet<>(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));

        HashSet<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(distinctEvent2.eventKey()));
        expectedEvents.add(distinctEvent1);

        Assert.assertEquals(expectedEvents, actualEvents);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_an_event_without_subscribing_for_it(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(401);
        EventKey eventKey = event1.eventKey();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        Event event = baseProperties.jSubscriber().get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(event1, event);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_InvalidEvent(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = EventKey.apply(Prefix.apply("test"), EventName.apply("test"));
        Event event = baseProperties.jSubscriber().get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertTrue(((SystemEvent) event).isInvalid());
        Assert.assertEquals(Event$.MODULE$.invalidEvent(eventKey), event);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_events_for_multiple_event_keys(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(306);
        EventKey eventKey1 = event1.eventKey();

        Event event2 = Utils.makeDistinctEvent(307);
        EventKey eventKey2 = event2.eventKey();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        HashSet<EventKey> keys = new HashSet<>();
        keys.add(eventKey1);
        keys.add(eventKey2);

        CompletableFuture<Set<Event>> eventsF = baseProperties.jSubscriber().get(keys);

        HashSet<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(eventKey2));
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, eventsF.get(10, TimeUnit.SECONDS));
    }
}
