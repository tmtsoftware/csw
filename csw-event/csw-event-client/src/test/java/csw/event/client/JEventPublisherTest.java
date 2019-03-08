package csw.event.client;

import akka.actor.Cancellable;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.japi.function.Procedure;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import csw.event.api.javadsl.IEventPublisher;
import csw.event.api.javadsl.IEventSubscription;
import csw.event.client.helpers.Utils;
import csw.event.client.internal.kafka.KafkaTestProps;
import csw.event.client.internal.redis.RedisTestProps;
import csw.event.client.internal.wiring.BaseProperties;
import csw.params.core.models.Prefix;
import csw.params.events.Event;
import csw.params.events.Event$;
import csw.params.events.EventKey;
import csw.time.core.models.UTCTime;
import org.scalatestplus.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-349: Event Service API creation
//DEOPSCSW-395: Provide EventService handle to component developers
public class JEventPublisherTest extends TestNGSuite {

    private RedisTestProps redisTestProps;
    private KafkaTestProps kafkaTestProps;

    private int counter = -1;
    private Cancellable cancellable;

    @BeforeSuite
    public void beforeAll() {
        redisTestProps = RedisTestProps.jCreateRedisProperties();
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties();
        redisTestProps.start();
        kafkaTestProps.start();
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

    //DEOPSCSW-345: Publish events irrespective of subscriber existence
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_and_subscribe_an_event(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctJavaEvent(new Random().nextInt());
        EventKey eventKey = event1.eventKey();

        TestProbe probe = TestProbe.create(baseProperties.typedActorSystem());

        Set<EventKey> set = Set.of(eventKey);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation

        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(set).take(2).toMat(Sink.foreach(event -> probe.ref().tell(event)), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    //DEOPSCSW-345: Publish events irrespective of subscriber existence
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_an_event_with_duration(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        List<Event> events = new ArrayList<>();
        for (int i = 11; i < 21; i++) {
            events.add(Utils.makeEvent(i));
        }

        EventKey eventKey = Utils.makeEvent(0).eventKey();

        List<Event> queue = new ArrayList<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for getting the latest event

        IEventPublisher publisher = baseProperties.jPublisher();
        counter = -1;
        cancellable = publisher.publish(() -> {
            counter += 1;
            return Optional.ofNullable(events.get(counter));
        }, Duration.ofMillis(10));

        Thread.sleep(1000);
        cancellable.cancel();

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(11, queue.size());

        events.add(0, Event$.MODULE$.invalidEvent(eventKey));
        Assert.assertEquals(events, queue);
    }

    //DEOPSCSW-341: Allow to reuse single connection for subscribing to multiple EventKeys
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_concurrently_to_the_different_channel(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        List<Event> events = new ArrayList<>();
        for (int i = 101; i < 111; i++) {
            events.add(Utils.makeDistinctJavaEvent(i));
        }

        Set<Event> queue = new HashSet<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(events.stream().map(Event::eventKey).collect(Collectors.toSet())).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(Source.fromIterator(events::iterator));
        Thread.sleep(1000);

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(20, queue.size());

        List<Event> expectedEvents = events.stream().map(event -> Event$.MODULE$.invalidEvent(event.eventKey())).collect(Collectors.toList());
        expectedEvents.addAll(events);
        Assert.assertEquals(Set.copyOf(expectedEvents), queue);
    }

    //DEOPSCSW-000: Publish events with block generating futre of event
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_an_event_with_block_generating_future_of_event_with_duration(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        List<Event> events = new ArrayList<>();
        for (int i = 31; i < 41; i++) {
            events.add(Utils.makeEventWithPrefix(i, new Prefix("test")));
        }

        EventKey eventKey = events.get(0).eventKey();

        List<Event> queue = new ArrayList<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for getting the latest event

        IEventPublisher publisher = baseProperties.jPublisher();
        counter = -1;
        cancellable = publisher.publishAsync(() -> {
            counter += 1;
            return CompletableFuture.completedFuture(Optional.ofNullable(events.get(counter)));
        }, Duration.ofMillis(10));

        Thread.sleep(1000);
        cancellable.cancel();

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(11, queue.size());

        events.add(0, Event$.MODULE$.invalidEvent(eventKey));
        Assert.assertEquals(events, queue);
    }

    //DEOPSCSW-595: Enforce ordering in publish
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_maintain_ordering_while_publish(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        Prefix prefix = Prefix.apply("ordering.test.prefix");
        Event event1             = Utils.makeEventWithPrefix(6, prefix);
        Event event2             = Utils.makeEventWithPrefix(7, prefix);
        Event event3             = Utils.makeEventWithPrefix(8, prefix);
        Event event4             = Utils.makeEventWithPrefix(9, prefix);
        Event event5             = Utils.makeEventWithPrefix(10, prefix);

        EventKey eventKey = event1.eventKey();
        TestProbe testProbe          = TestProbe.create(baseProperties.typedActorSystem());

        IEventSubscription subscription = baseProperties.jSubscriber()
                .subscribe(Collections.singleton(eventKey))
                .toMat(Sink.foreach((Procedure<Event>) testProbe.ref()::tell), Keep.left())
                .run(baseProperties.resumingMat());

        subscription.ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        baseProperties.jPublisher().publish(event1);
        baseProperties.jPublisher().publish(event2);
        baseProperties.jPublisher().publish(event3);
        baseProperties.jPublisher().publish(event4);
        baseProperties.jPublisher().publish(event5);

        Thread.sleep(1000);

        testProbe.expectMessage(Event$.MODULE$.invalidEvent(eventKey));
        testProbe.expectMessage(event1);
        testProbe.expectMessage(event2);
        testProbe.expectMessage(event3);
        testProbe.expectMessage(event4);
        testProbe.expectMessage(event5);
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_event_via_event_generator_with_start_time(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {

        List<Event> events = new ArrayList<>();
        for(int i = 31; i < 41; i++){
            events.add(Utils.makeEventWithPrefix(i, new Prefix("start.time.test.publish")));
        }

        EventKey eventKey = events.get(0).eventKey();

        List<Event> queue = new ArrayList<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for getting the latest event

        IEventPublisher publisher = baseProperties.jPublisher();
        counter = -1;
        cancellable = publisher.publish(() -> {
            counter += 1;
            return Optional.ofNullable(events.get(counter));
        },new UTCTime(UTCTime.now().value().plusSeconds(1)), Duration.ofMillis(300));


        Thread.sleep(2000);
        cancellable.cancel();

        Assert.assertEquals(queue.size(), 5);

        events.add(0, Event$.MODULE$.invalidEvent(eventKey));
        Assert.assertTrue(queue.containsAll(events.subList(0,5)));
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_event_via_asynchrnous_event_generator_with_start_time(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {

        List<Event> events = new ArrayList<>();
        for(int i = 31; i < 41; i++){
            events.add(Utils.makeEventWithPrefix(i, new Prefix("start.time.test.publishAsync")));
        }

        EventKey eventKey = events.get(0).eventKey();

        List<Event> queue = new ArrayList<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for getting the latest event

        IEventPublisher publisher = baseProperties.jPublisher();
        counter = -1;
        cancellable = publisher.publishAsync(() -> {
            counter += 1;
            return CompletableFuture.completedFuture(Optional.ofNullable(events.get(counter)));
        },new UTCTime(UTCTime.now().value().plusSeconds(1)), Duration.ofMillis(300));


        Thread.sleep(2000);
        cancellable.cancel();

        Assert.assertEquals(queue.size(), 5);

        events.add(0, Event$.MODULE$.invalidEvent(eventKey));
        Assert.assertTrue(queue.containsAll(events.subList(0,5)));
    }
}
