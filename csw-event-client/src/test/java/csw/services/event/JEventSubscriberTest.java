package csw.services.event;

import akka.actor.Cancellable;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.testkit.typed.javadsl.TestProbe;
import csw.messages.events.*;
import csw.messages.javadsl.JSubsystem;
import csw.messages.params.models.Prefix;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.kafka.KafkaTestProps;
import csw.services.event.internal.redis.RedisTestProps;
import csw.services.event.internal.wiring.BaseProperties;
import csw.services.event.javadsl.IEventSubscription;
import csw.services.event.scaladsl.SubscriptionModes;
import io.lettuce.core.ClientOptions;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-395: Provide EventService handle to component developers
public class JEventSubscriberTest extends TestNGSuite {

    private RedisTestProps redisTestProps;
    private KafkaTestProps kafkaTestProps;

    @BeforeSuite
    public void beforeAll() {
        redisTestProps = RedisTestProps.createRedisProperties(4564, 27382, 7382, ClientOptions.create());
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties(4565, 7003, Collections.EMPTY_MAP);
        redisTestProps.start();
        kafkaTestProps.start();
    }

    public List<Event> getEvents() {
        List<Event> events = new ArrayList<>();
        for(int i = 0; i < 1500; i++) {
            events.add(Utils.makeEvent(i));
        }
        return events;
    }

    private int counter = 0;
    private List<Event> events = getEvents();

    private Supplier<Event> eventGenerator() {
        return () -> events.get(counter++);
    }

    @AfterSuite
    public void afterAll() {
        redisTestProps.shutdown();
        kafkaTestProps.shutdown();
    }

    @DataProvider(name = "event-service-provider")
    public Object[] pubsubProvider() {
        return new Object[]{redisTestProps, kafkaTestProps};
    }

    @DataProvider(name = "redis-provider")
    public Object[] redisPubSubProvider() {
        return new Object[]{redisTestProps};
    }


    //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
    //DEOPSCSW-343: Unsubscribe based on prefix and event name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_an_event(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(new Random().nextInt());
        EventKey eventKey = event1.eventKey();

        TestProbe probe = TestProbe.create(baseProperties.typedActorSystem());

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(set).take(2).toMat(Sink.foreach(event -> probe.ref().tell(event)), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);

        probe.expectMessage(Event$.MODULE$.invalidEvent(eventKey));
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    //DEOPSCSW-343: Unsubscribe based on prefix and event name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_async_callback(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(new Random().nextInt());

        TestProbe probe = TestProbe.create(baseProperties.typedActorSystem());

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
        Event event1 = Utils.makeEvent(1);
        List<Event> queue = new ArrayList<>();
        List<Event> queue2 = new ArrayList<>();

        counter = 0;
        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator(), new FiniteDuration(1, TimeUnit.MILLISECONDS));

        IEventSubscription subscription = baseProperties.jSubscriber().subscribeAsync(Collections.singleton(event1.eventKey()), event -> {
            queue.add(event);
            return CompletableFuture.completedFuture(event);
        }, Duration.ofMillis(300), SubscriptionModes.jRateAdapterMode());

        IEventSubscription subscription2 = baseProperties.jSubscriber().subscribeAsync(Collections.singleton(event1.eventKey()), event -> {
            queue2.add(event);
            return CompletableFuture.completedFuture(event);
        }, Duration.ofMillis(400), SubscriptionModes.jRateAdapterMode());

        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription2.unsubscribe().get(10, TimeUnit.SECONDS);

        cancellable.cancel();
        Assert.assertEquals(queue.size(), 4);
        Assert.assertEquals(queue2.size(), 3);
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_callback(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {

        TestProbe probe = TestProbe.create(baseProperties.typedActorSystem());

        List<Event> listOfPublishedEvents = new ArrayList<>(5);
        for(int i = 1; i <= 5; i ++) {
            Event event = Utils.makeEvent(i);
            listOfPublishedEvents.add(event);
            baseProperties.jPublisher().publish(event).get(10, TimeUnit.SECONDS);
        }

        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeCallback(Collections.singleton(listOfPublishedEvents.get(0).eventKey()), event -> probe.ref().tell(event));
        probe.expectMessage(listOfPublishedEvents.get(4));

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(listOfPublishedEvents.get(0)).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_callback_with_duration(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeEvent(1);
        List<Event> queue = new ArrayList<>();
        List<Event> queue2 = new ArrayList<>();

        counter = 0;
        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator(), new FiniteDuration(1, TimeUnit.MILLISECONDS));
        Thread.sleep(500);
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeCallback(Collections.singleton(event1.eventKey()), queue::add, Duration.ofMillis(300), SubscriptionModes.jRateAdapterMode());

        IEventSubscription subscription2 = baseProperties.jSubscriber().subscribeCallback(Collections.singleton(event1.eventKey()), queue2::add, Duration.ofMillis(400), SubscriptionModes.jRateAdapterMode());

        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription2.unsubscribe().get(10, TimeUnit.SECONDS);

        cancellable.cancel();
        Assert.assertEquals(queue.size(), 4);
        Assert.assertEquals(queue2.size(), 3);
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_an_ActorRef(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(new Random().nextInt());

        TestProbe probe = TestProbe.create(baseProperties.typedActorSystem());

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Collections.singleton(event1.eventKey()), probe.ref());
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_an_ActorRef_with_duration(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(new Random().nextInt());

        TestInbox<Event> inbox = TestInbox.create();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Collections.singleton(event1.eventKey()), inbox.getRef(), Duration.ofMillis(300), SubscriptionModes.jRateAdapterMode());
        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        Assert.assertEquals(inbox.getAllReceived().size(), 4);
    }

    // DEOPSCSW-420: Implement Pattern based subscription
    // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
    @Test(dataProvider = "redis-provider")
    public void should_be_able_to_subscribe_an_event_with_pattern_from_different_subsystem(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event testEvent1 = Utils.makeEventWithPrefix(1, new Prefix("test.prefix"));
        Event testEvent2 = Utils.makeEventWithPrefix(2, new Prefix("test.prefix"));
        Event tcsEvent1 = Utils.makeEventWithPrefix(1, new Prefix("tcs.prefix"));

        TestProbe<Event> probe = TestProbe.create(baseProperties.typedActorSystem());

        // pattern is * for redis
        IEventSubscription subscription = baseProperties.jSubscriber().pSubscribe(JSubsystem.TEST, baseProperties.eventPattern(), event -> probe.ref().tell(event));
        subscription.ready().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(testEvent1).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent2).get(10, TimeUnit.SECONDS);

        probe.expectMessage(testEvent1);
        probe.expectMessage(testEvent2);

        baseProperties.jPublisher().publish(tcsEvent1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofSeconds(2));

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
    }

    // DEOPSCSW-420: Implement Pattern based subscription
    // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
    @Test(dataProvider = "redis-provider")
    public void should_be_able_to_subscribe_an_event_with_pattern_from_same_subsystem(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event testEvent1 = Utils.makeEventForKeyName(new EventName("movement.linear"),1);
        Event testEvent2 = Utils.makeEventForKeyName(new EventName("movement.angular"),2);
        Event testEvent3 = Utils.makeEventForKeyName(new EventName("temperature"),3);
        Event testEvent4 = Utils.makeEventForKeyName(new EventName("move"),3);
        Event testEvent5 = Utils.makeEventForKeyName(new EventName("cove"),3);
        Event testEvent6 = Utils.makeEventForPrefixAndKeyName(new Prefix("test.test_prefix"), new EventName("move"), 6);

        TestInbox<Event> inbox = TestInbox.create();
        TestInbox<Event> inbox2 = TestInbox.create();
        TestInbox<Event> inbox3 = TestInbox.create();
        TestInbox<Event> inbox4 = TestInbox.create();
        TestInbox<Event> inbox5 = TestInbox.create();

        String eventPattern  = "*.movement.*";
        String eventPattern2 = "*.move*";
        String eventPattern3 = "*.?ove*";
        String eventPattern4 = "test_prefix.*";
        String eventPattern5 = "*";

        // pattern is * for redis
        IEventSubscription subscription = baseProperties.jSubscriber().pSubscribe(JSubsystem.TEST, eventPattern, event -> inbox.getRef().tell(event));
        IEventSubscription subscription2 = baseProperties.jSubscriber().pSubscribe(JSubsystem.TEST, eventPattern2, event -> inbox2.getRef().tell(event));
        IEventSubscription subscription3 = baseProperties.jSubscriber().pSubscribe(JSubsystem.TEST, eventPattern3, event -> inbox3.getRef().tell(event));
        IEventSubscription subscription4 = baseProperties.jSubscriber().pSubscribe(JSubsystem.TEST, eventPattern4, event -> inbox4.getRef().tell(event));
        IEventSubscription subscription5 = baseProperties.jSubscriber().pSubscribe(JSubsystem.TEST, eventPattern5, event -> inbox5.getRef().tell(event));

        subscription.ready().get(10, TimeUnit.SECONDS);
        subscription2.ready().get(10, TimeUnit.SECONDS);
        subscription3.ready().get(10, TimeUnit.SECONDS);
        subscription4.ready().get(10, TimeUnit.SECONDS);
        subscription5.ready().get(10, TimeUnit.SECONDS);

        Thread.sleep(500);

        baseProperties.jPublisher().publish(testEvent1).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent2).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent3).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent4).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent5).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent6).get(10, TimeUnit.SECONDS);

        Thread.sleep(1000);

        List<Event> receivedEvents = inbox.getAllReceived();
        List<Event> receivedEvents2 = inbox2.getAllReceived();
        List<Event> receivedEvents3 = inbox3.getAllReceived();
        List<Event> receivedEvents4 = inbox4.getAllReceived();
        List<Event> receivedEvents5 = inbox5.getAllReceived();

        Assert.assertTrue(receivedEvents.containsAll(Arrays.asList(testEvent1, testEvent2)));
        Assert.assertTrue(receivedEvents2.containsAll(Arrays.asList(testEvent1, testEvent2, testEvent4, testEvent6)));
        Assert.assertTrue(receivedEvents3.containsAll(Arrays.asList(testEvent4, testEvent5, testEvent6)));
        Assert.assertTrue(receivedEvents4.contains(testEvent6));
        Assert.assertTrue(receivedEvents5.containsAll(Arrays.asList(testEvent1, testEvent2, testEvent3, testEvent4, testEvent5, testEvent6)));

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription2.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription3.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription4.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription5.unsubscribe().get(10, TimeUnit.SECONDS);
    }

    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_make_independent_subscriptions(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Prefix prefix = new Prefix("test.prefix");
        EventName eventName1 = new EventName("system1");
        EventName eventName2 = new EventName("system2");
        Event event1 = new SystemEvent(prefix, eventName1);
        Event event2 = new SystemEvent(prefix, eventName2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Collections.singleton(event1.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.resumingMat());
        pair.first().ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(100);
        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair2 = baseProperties.jSubscriber().subscribe(Collections.singleton(event2.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.resumingMat());
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

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.resumingMat());
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

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).take(1).toMat(Sink.seq(), Keep.both()).run(baseProperties.resumingMat());

        Assert.assertEquals(Collections.singletonList(Event$.MODULE$.invalidEvent(eventKey)), pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_valid_as_well_as_invalid_event_when_events_are_published_for_some_and_not_for_other_keys(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event distinctEvent1 = Utils.makeDistinctEvent(new Random().nextInt());
        Event distinctEvent2 = Utils.makeDistinctEvent(new Random().nextInt());

        EventKey eventKey1 = distinctEvent1.eventKey();
        EventKey eventKey2 = distinctEvent2.eventKey();

        baseProperties.jPublisher().publish(distinctEvent1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        Set<EventKey> eventKeys = new HashSet<>();
        eventKeys.add(eventKey1);
        eventKeys.add(eventKey2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(eventKeys).take(2).toMat(Sink.seq(), Keep.both()).run(baseProperties.resumingMat());

        Set<Event> actualEvents = new HashSet<>(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));

        HashSet<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(distinctEvent2.eventKey()));
        expectedEvents.add(distinctEvent1);

        Assert.assertEquals(expectedEvents, actualEvents);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_an_event_without_subscribing_for_it(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(new Random().nextInt());
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
        Event event1 = Utils.makeDistinctEvent(new Random().nextInt());
        EventKey eventKey1 = event1.eventKey();

        Event event2 = Utils.makeDistinctEvent(new Random().nextInt());
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
