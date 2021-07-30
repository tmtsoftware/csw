package csw.event.client;

import akka.actor.Cancellable;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import csw.event.api.javadsl.IEventSubscription;
import csw.event.api.scaladsl.SubscriptionModes;
import csw.event.client.helpers.Utils;
import csw.event.client.internal.kafka.KafkaTestProps;
import csw.event.client.internal.redis.RedisTestProps;
import csw.event.client.internal.wiring.BaseProperties;
import csw.logging.client.utils.Eventually;
import csw.params.core.models.ObsId;
import csw.params.events.*;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.scalatestplus.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static csw.prefix.javadsl.JSubsystem.IRIS;
import static csw.prefix.javadsl.JSubsystem.WFOS;

//DEOPSCSW-331: Event Service Accessible to all CSW component builders
//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-349: Event Service API creation
//DEOPSCSW-395: Provide EventService handle to component developers
public class JEventSubscriberTest extends TestNGSuite {

    private RedisTestProps redisTestProps;
    private KafkaTestProps kafkaTestProps;

    @BeforeSuite
    public void beforeAll() {
        redisTestProps = RedisTestProps.jCreateRedisProperties();
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties();
        redisTestProps.start();
        kafkaTestProps.start();
    }

    public List<Event> getEvents() {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            events.add(Utils.makeEvent(i));
        }
        return events;
    }

    private int counter = 0;
    private List<Event> events = getEvents();

    private Supplier<Optional<Event>> eventGenerator() {
        return () -> Optional.ofNullable(events.get(counter++));
    }

    @AfterSuite
    public void afterAll() {
        redisTestProps.shutdown();
        kafkaTestProps.shutdown();
    }

    @DataProvider(name = "event-service-provider")
    public Object[][] pubsubProvider() {
        return new Object[][]{{redisTestProps}, {kafkaTestProps}};
    }

    @DataProvider(name = "redis-provider")
    public Object[][] redisPubSubProvider() {
        return new Object[][]{{redisTestProps}};
    }


    //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_an_event__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_346(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());
        EventKey eventKey = event1.eventKey();

        TestProbe probe = TestProbe.create(baseProperties.actorSystem());

        java.util.Set<EventKey> set = Set.of(eventKey);

        IEventSubscription subscription =
                baseProperties.jSubscriber().subscribe(set)
                        .take(2)
                        .toMat(Sink.foreach(event -> probe.ref().tell(event)), Keep.left())
                        .withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());

        subscription.ready().get(10, TimeUnit.SECONDS);
        probe.expectMessage(Event$.MODULE$.invalidEvent(eventKey));

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectMessage(event1);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    //DEOPSCSW-343: Unsubscribe based on prefix and event name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_async_callback__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_343(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());

        TestProbe probe = TestProbe.create(baseProperties.actorSystem());

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeAsync(Set.of(event1.eventKey()), event -> {
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
    public void should_be_able_to_subscribe_with_async_callback_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_342(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeEvent(1);
        List<Event> queue = new ArrayList<>();
        List<Event> queue2 = new ArrayList<>();

        counter = 0;
        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator(), Duration.ofMillis(1));

        IEventSubscription subscription = baseProperties.jSubscriber().subscribeAsync(Set.of(event1.eventKey()), event -> {
            queue.add(event);
            return CompletableFuture.completedFuture(event);
        }, Duration.ofMillis(300), SubscriptionModes.jRateAdapterMode());

        IEventSubscription subscription2 = baseProperties.jSubscriber().subscribeAsync(Set.of(event1.eventKey()), event -> {
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
    public void should_be_able_to_subscribe_with_callback__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {

        TestProbe probe = TestProbe.create(baseProperties.actorSystem());

        List<Event> listOfPublishedEvents = new ArrayList<>(5);
        for (int i = 1; i <= 5; i++) {
            Event event = Utils.makeEvent(i);
            listOfPublishedEvents.add(event);
            baseProperties.jPublisher().publish(event).get(10, TimeUnit.SECONDS);
        }

        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeCallback(Set.of(listOfPublishedEvents.get(0).eventKey()), event -> probe.ref().tell(event));
        probe.expectMessage(listOfPublishedEvents.get(4));

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(listOfPublishedEvents.get(0)).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_callback_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_342(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeEvent(1);
        List<Event> queue = new ArrayList<>();
        List<Event> queue2 = new ArrayList<>();

        counter = 0;
        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator(), Duration.ofMillis(1));
        Thread.sleep(500);
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeCallback(Set.of(event1.eventKey()), queue::add, Duration.ofMillis(300), SubscriptionModes.jRateAdapterMode());

        IEventSubscription subscription2 = baseProperties.jSubscriber().subscribeCallback(Set.of(event1.eventKey()), queue2::add, Duration.ofMillis(400), SubscriptionModes.jRateAdapterMode());

        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription2.unsubscribe().get(10, TimeUnit.SECONDS);

        cancellable.cancel();
        Assert.assertEquals(queue.size(), 4);
        Assert.assertEquals(queue2.size(), 3);
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_an_ActorRef__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_339(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());

        TestProbe probe = TestProbe.create(baseProperties.actorSystem());

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Set.of(event1.eventKey()), probe.ref());
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_an_ActorRef_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_339_DEOPSCSW_342(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());

        TestInbox<Event> inbox = TestInbox.create();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Set.of(event1.eventKey()), inbox.getRef(), Duration.ofMillis(300), SubscriptionModes.jRateAdapterMode());
        Thread.sleep(1000);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        Assert.assertEquals(inbox.getAllReceived().size(), 4);
    }

    // DEOPSCSW-420: Implement Pattern based subscription
    // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
    @Test(dataProvider = "redis-provider")
    public void should_be_able_to_subscribe_an_event_with_pattern_from_different_subsystem__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_420(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event testEvent1 = Utils.makeEventWithPrefix(1, Prefix.apply(JSubsystem.CSW, "prefix"));
        Event testEvent2 = Utils.makeEventWithPrefix(2, Prefix.apply(JSubsystem.CSW, "prefix"));
        Event tcsEvent1 = Utils.makeEventWithPrefix(1, Prefix.apply(JSubsystem.TCS, "prefix"));

        TestProbe<Event> probe = TestProbe.create(baseProperties.actorSystem());

        // pattern is * for redis
        IEventSubscription subscription = baseProperties.jSubscriber().pSubscribeCallback(JSubsystem.CSW, baseProperties.eventPattern(), event -> probe.ref().tell(event));
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
    public void should_be_able_to_subscribe_an_event_with_pattern_from_same_subsystem__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_420(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event testEvent1 = Utils.makeEventForKeyName(new EventName("movement.linear"), 1);
        Event testEvent2 = Utils.makeEventForKeyName(new EventName("movement.angular"), 2);
        Event testEvent3 = Utils.makeEventForKeyName(new EventName("temperature"), 3);
        Event testEvent4 = Utils.makeEventForKeyName(new EventName("move"), 3);
        Event testEvent5 = Utils.makeEventForKeyName(new EventName("cove"), 3);
        Event testEvent6 = Utils.makeEventForPrefixAndKeyName(Prefix.apply(JSubsystem.CSW, "test_prefix"), new EventName("move"), 6);

        TestInbox<Event> inbox = TestInbox.create();
        TestInbox<Event> inbox2 = TestInbox.create();
        TestInbox<Event> inbox3 = TestInbox.create();
        TestInbox<Event> inbox4 = TestInbox.create();
        TestInbox<Event> inbox5 = TestInbox.create();

        String eventPattern = "*.movement.*";      //subscribe to events with any prefix but event name containing 'movement'
        String eventPattern2 = "*.move*";           //subscribe to events with any prefix but event name containing 'move'
        String eventPattern3 = "*.?ove*";           //subscribe to events with any prefix but event name matching any first  character followed by `ove`
        String eventPattern4 = "test_prefix.*";     //subscribe to all events with prefix `test_prefix` irresepective of event names
        String eventPattern5 = "*";                 //subscribe to all events with prefix `test_prefix` irresepective of event names

        // pattern is * for redis
        IEventSubscription subscription = baseProperties.jSubscriber().pSubscribeCallback(JSubsystem.CSW, eventPattern, event -> inbox.getRef().tell(event));
        IEventSubscription subscription2 = baseProperties.jSubscriber().pSubscribeCallback(JSubsystem.CSW, eventPattern2, event -> inbox2.getRef().tell(event));
        IEventSubscription subscription3 = baseProperties.jSubscriber().pSubscribeCallback(JSubsystem.CSW, eventPattern3, event -> inbox3.getRef().tell(event));
        IEventSubscription subscription4 = baseProperties.jSubscriber().pSubscribeCallback(JSubsystem.CSW, eventPattern4, event -> inbox4.getRef().tell(event));
        IEventSubscription subscription5 = baseProperties.jSubscriber().pSubscribeCallback(JSubsystem.CSW, eventPattern5, event -> inbox5.getRef().tell(event));

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

    // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
    @Test(dataProvider = "redis-provider")
    public void should_be_able_to_subscribe_all_observe_events_CSW_119(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        ObsId obsId = ObsId.apply("2020A-001-123");
        Event irDetObsStart = IRDetectorEvent.observeStart(new Prefix(IRIS,"det"), obsId);
        Event irDetObsEnd = IRDetectorEvent.observeEnd(new Prefix(IRIS,"det"), obsId);
        Event publishSuccess = WFSDetectorEvent.publishSuccess(new Prefix(WFOS,"test"));
        Event optDetObsStart = OpticalDetectorEvent.observeStart(new Prefix(WFOS,"det"), obsId);
        Event testEvent = Utils.makeEventWithPrefix(1, Prefix.apply(JSubsystem.CSW, "prefix"));

        List<Event> receivedEvents = new ArrayList<>();

        IEventSubscription subscription =
                baseProperties.jSubscriber().subscribeObserveEvents().to(Sink.foreach(receivedEvents::add))
                        .withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());

        subscription.ready().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(irDetObsStart).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(irDetObsEnd).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(publishSuccess).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(optDetObsStart).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(testEvent).get(10, TimeUnit.SECONDS);

        Eventually.eventually(Duration.ofSeconds(5), () -> Assert.assertEquals(receivedEvents, List.of(irDetObsStart, irDetObsEnd, publishSuccess, optDetObsStart)));
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
    }

    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_make_independent_subscriptions__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Prefix prefix = Prefix.apply(JSubsystem.CSW, "prefix");
        EventName eventName1 = new EventName("system1");
        EventName eventName2 = new EventName("system2");
        Event event1 = new SystemEvent(prefix, eventName1);
        Event event2 = new SystemEvent(prefix, eventName2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Set.of(event1.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());
        pair.first().ready().get(10, TimeUnit.SECONDS);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair2 = baseProperties.jSubscriber().subscribe(Set.of(event2.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());
        pair2.first().ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(event2).get(10, TimeUnit.SECONDS);

        Set<Event> expectedEvents = Set.of(Event$.MODULE$.invalidEvent(event1.eventKey()), event1);

        Set<Event> expectedEvents2 = Set.of(Event$.MODULE$.invalidEvent(event2.eventKey()), event2);

        Assert.assertEquals(expectedEvents, Set.copyOf(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS)));

        Assert.assertEquals(expectedEvents2, Set.copyOf(pair2.second().toCompletableFuture().get(10, TimeUnit.SECONDS)));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_recently_published_event_on_subscription__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_340(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        Event event2 = Utils.makeEvent(2);
        Event event3 = Utils.makeEvent(3);
        EventKey eventKey = event1.eventKey();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        baseProperties.jPublisher().publish(event2).get(10, TimeUnit.SECONDS); // latest event before subscribing
        Thread.sleep(500); // Needed for redis set which is fire and forget operation

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Set.of(eventKey)).take(2).toMat(Sink.seq(), Keep.both()).withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());
        pair.first().ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        baseProperties.jPublisher().publish(event3).get(10, TimeUnit.SECONDS);
        java.util.List<Event> expectedEvents = List.of(event2, event3);

        // assertion against a list ensures that the latest event before subscribing arrives earlier in the stream
        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_InvalidEvent__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_340(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = EventKey.apply(Prefix.apply("csw.invalid"), EventName.apply("test"));

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(Set.of(eventKey)).take(1).toMat(Sink.seq(), Keep.both()).withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());

        Assert.assertEquals(List.of(Event$.MODULE$.invalidEvent(eventKey)), pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_retrieve_valid_as_well_as_invalid_event_when_events_are_published_for_some_and_not_for_other_keys__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_340(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event distinctEvent1 = Utils.makeDistinctJavaEvent(new Random().nextInt());
        Event distinctEvent2 = Utils.makeDistinctJavaEvent(new Random().nextInt());

        EventKey eventKey1 = distinctEvent1.eventKey();
        EventKey eventKey2 = distinctEvent2.eventKey();

        baseProperties.jPublisher().publish(distinctEvent1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        Set<EventKey> eventKeys = Set.of(eventKey1, eventKey2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = baseProperties.jSubscriber().subscribe(eventKeys).take(2).toMat(Sink.seq(), Keep.both()).withAttributes(baseProperties.attributes()).run(baseProperties.actorSystem());

        Set<Event> actualEvents = Set.copyOf(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));

        Set<Event> expectedEvents = Set.of(Event$.MODULE$.invalidEvent(distinctEvent2.eventKey()), distinctEvent1);

        Assert.assertEquals(expectedEvents, actualEvents);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_an_event_without_subscribing_for_it__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_344(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());
        EventKey eventKey = event1.eventKey();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        Event event = baseProperties.jSubscriber().get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(event1, event);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_InvalidEvent__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_344(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = EventKey.apply(Prefix.apply("csw.invalid"), EventName.apply("test"));
        Event event = baseProperties.jSubscriber().get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertTrue(event.isInvalid());
        Assert.assertEquals(Event$.MODULE$.invalidEvent(eventKey), event);
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_get_events_for_multiple_event_keys__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_344(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());
        EventKey eventKey1 = event1.eventKey();

        Event event2 = Utils.makeDistinctJavaEvent(new Random().nextInt());
        EventKey eventKey2 = event2.eventKey();

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation
        Set<EventKey> keys = Set.of(eventKey1, eventKey2);

        CompletableFuture<Set<Event>> eventsF = baseProperties.jSubscriber().get(keys);

        Set<Event> expectedEvents = Set.of(Event$.MODULE$.invalidEvent(eventKey2), event1);

        Assert.assertEquals(expectedEvents, eventsF.get(10, TimeUnit.SECONDS));
    }
}
