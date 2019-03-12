package csw.event.client;

import akka.actor.Cancellable;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import csw.event.api.javadsl.IEventSubscription;
import csw.event.api.scaladsl.SubscriptionModes;
import csw.event.client.helpers.Utils;
import csw.event.client.internal.kafka.KafkaTestProps;
import csw.event.client.internal.redis.RedisTestProps;
import csw.event.client.internal.wiring.BaseProperties;
import csw.params.events.Event;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import org.scalatestplus.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-395: Provide EventService handle to component developers
public class JEventSubscriptionFrequencyTest extends TestNGSuite {

    private RedisTestProps redisTestProps;
    private KafkaTestProps kafkaTestProps;

    @BeforeSuite
    public void beforeAll() {
        redisTestProps = RedisTestProps.jCreateRedisProperties();
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties();
        redisTestProps.start();
        kafkaTestProps.start();
    }

    private List<Event> getEventsWithName(EventName eventName) {
        List<Event> events = new ArrayList<>();
        for(int i = 0; i < 1500; i++) {
            events.add(Utils.makeEventForKeyName(eventName, i));
        }
        return events;
    }

    private int counter = 0;

    class EventGenerator {
        EventName eventName;
        List<Event> publishedEvents;
        List<Event> eventsGroup;

        EventGenerator(EventName eventName) {
            this.eventName = eventName;
            counter = 0;
            this.publishedEvents = new ArrayList<>();
            this.eventsGroup = getEventsWithName(eventName);
        }

        Supplier<Optional<Event>> generator() {
                return () -> {
                    Event event = eventsGroup.get(counter++);
                    publishedEvents.add(event);
                    return Optional.ofNullable(event);
                };
        }
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

    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_duration_with_rate_adapter_mode_for_slow_publisher(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventGenerator eventGenerator = new EventGenerator(new EventName("system_"+ new Random().nextInt()));
        EventKey eventKey = eventGenerator.eventsGroup.get(0).eventKey();

        TestInbox<Event> inbox = TestInbox.create();

        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator.generator(), Duration.ofMillis(400));
        Thread.sleep(500);
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Set.of(eventKey), inbox.getRef(), Duration.ofMillis(100), SubscriptionModes.jRateAdapterMode());
        Thread.sleep(1050);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        cancellable.cancel();

        List<Event> eventsReceived = inbox.getAllReceived();

        Assert.assertEquals(eventsReceived.size(), 11);
        Assert.assertTrue(eventGenerator.publishedEvents.containsAll(eventsReceived));
        // assert that received elements will have duplicates
        Assert.assertNotEquals(eventsReceived.stream().distinct().count(), 11);
    }

    //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_duration_with_rate_adapter_for_fast_publisher(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {

        EventGenerator eventGenerator = new EventGenerator(new EventName("system_"+ new Random().nextInt()));
        EventKey eventKey = eventGenerator.eventsGroup.get(0).eventKey();

        List<Event> receivedEvents = new ArrayList<>();
        List<Event> receivedEvents2 = new ArrayList<>();

        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator.generator(), Duration.ofMillis(100));
        Thread.sleep(500);
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(Set.of(eventKey), Duration.ZERO.plusMillis(600), SubscriptionModes.jRateAdapterMode()).toMat(Sink.foreach(receivedEvents::add), Keep.left()).run(baseProperties.resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        IEventSubscription subscription2 = baseProperties.jSubscriber().subscribe(Set.of(eventKey), Duration.ZERO.plusMillis(800), SubscriptionModes.jRateAdapterMode()).toMat(Sink.foreach(receivedEvents2::add), Keep.left()).run(baseProperties.resumingMat());
        subscription2.ready().get(10, TimeUnit.SECONDS);

        Thread.sleep(2000);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        subscription2.unsubscribe().get(10, TimeUnit.SECONDS);
        cancellable.cancel();

        Assert.assertEquals(receivedEvents.size(), 4);
        Assert.assertTrue(eventGenerator.publishedEvents.containsAll(receivedEvents));
        // assert if received elements do not have duplicates
        Assert.assertEquals(receivedEvents.stream().distinct().count(), 4);

        Assert.assertEquals(receivedEvents2.size(), 3);
        // assert if received elements do not have duplicates
        Assert.assertEquals(receivedEvents2.stream().distinct().count(), 3);
    }

    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_duration_with_rate_limiter_mode_for_slow_publisher(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventGenerator eventGenerator = new EventGenerator(new EventName("system_"+ new Random().nextInt()));
        EventKey eventKey = eventGenerator.eventsGroup.get(0).eventKey();
        TestInbox<Event> inbox = TestInbox.create();

        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator.generator(), Duration.ofMillis(400));
        Thread.sleep(500);
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Set.of(eventKey), inbox.getRef(), Duration.ofMillis(100), SubscriptionModes.jRateLimiterMode());
        Thread.sleep(900);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        cancellable.cancel();
        List<Event> receivedEvents = inbox.getAllReceived();
        Assert.assertEquals(receivedEvents.size(), 3);
        Assert.assertTrue(eventGenerator.publishedEvents.containsAll(receivedEvents));

        // assert if received elements do not have duplicates
        Assert.assertEquals(receivedEvents.stream().distinct().count(), 3);
    }

    //DEOPSCSW-342: Subscription with consumption frequency
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_subscribe_with_duration_with_rate_limiter_mode_for_fast_publisher(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        EventGenerator eventGenerator = new EventGenerator(new EventName("system_"+ new Random().nextInt()));
        EventKey eventKey = eventGenerator.eventsGroup.get(0).eventKey();

        TestInbox<Event> inbox = TestInbox.create();

        Cancellable cancellable = baseProperties.jPublisher().publish(eventGenerator.generator(), Duration.ofMillis(100));
        Thread.sleep(500);
        IEventSubscription subscription = baseProperties.jSubscriber().subscribeActorRef(Set.of(eventKey), inbox.getRef(), Duration.ofMillis(400), SubscriptionModes.jRateLimiterMode());
        Thread.sleep(1800);
        subscription.unsubscribe().get(10, TimeUnit.SECONDS);
        cancellable.cancel();

        List<Event> receivedEvents = inbox.getAllReceived();
        Assert.assertEquals(receivedEvents.size(), 5);
        Assert.assertTrue(eventGenerator.publishedEvents.containsAll(receivedEvents));

        // assert if received elements do not have duplicates
        Assert.assertEquals(receivedEvents.stream().distinct().count(), 5);
    }

}
