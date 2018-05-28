package csw.services.event;

import akka.actor.Cancellable;
import akka.actor.typed.javadsl.Adapter;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.typed.javadsl.TestProbe;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.*;
import csw.messages.params.models.Prefix;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.kafka.KafkaTestProps;
import csw.services.event.internal.redis.RedisTestProps;
import csw.services.event.internal.wiring.BaseProperties;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscription;
import io.lettuce.core.ClientOptions;
import net.manub.embeddedkafka.EmbeddedKafka$;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Function1;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
public class JEventPublisherTest extends TestNGSuite {

    private RedisTestProps redisTestProps;
    private KafkaTestProps kafkaTestProps;

    private int counter = -1;
    private Cancellable cancellable;

    @BeforeSuite
    public void beforeAll() {
        redisTestProps = RedisTestProps.createRedisProperties(4562, 27381, 7381, ClientOptions.create());
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties(4563, 7002, Collections.EMPTY_MAP);
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

    //DEOPSCSW-345: Publish events irrespective of subscriber existence
    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_and_subscribe_an_event(BaseProperties baseProperties) throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        TestProbe probe = TestProbe.create(Adapter.toTyped(baseProperties.wiring().actorSystem()));

        Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Needed for redis set which is fire and forget operation

        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(set).take(2).toMat(Sink.foreach(event -> probe.ref().tell(event)), Keep.left()).run(baseProperties.wiring().resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    @Test(dataProvider = "event-service-provider")
    public void should_be_able_to_publish_an_event_with_duration(BaseProperties baseProperties) throws InterruptedException, TimeoutException, ExecutionException {
        List<Event> events = new ArrayList<>();
        for (int i = 11; i < 21; i++) {
            events.add(Utils.makeEvent(i));
        }

        EventKey eventKey = Utils.makeEvent(0).eventKey();

        List<Event> queue = new ArrayList<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(Collections.singleton(eventKey)).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.wiring().resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        IEventPublisher publisher = baseProperties.jPublisher();
        counter = -1;
        cancellable = publisher.publish(() -> {
            counter += 1;
            return events.get(counter);
        }, new FiniteDuration(10, TimeUnit.MILLISECONDS));

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
            events.add(Utils.makeDistinctEvent(i));
        }

        Set<Event> queue = new HashSet<>();
        IEventSubscription subscription = baseProperties.jSubscriber().subscribe(events.stream().map(Event::eventKey).collect(Collectors.toSet())).toMat(Sink.foreach(queue::add), Keep.left()).run(baseProperties.wiring().resumingMat());
        subscription.ready().get(10, TimeUnit.SECONDS);

        baseProperties.jPublisher().publish(Source.fromIterator(events::iterator));
        Thread.sleep(1000);

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(20, queue.size());

        List<Event> expectedEvents = events.stream().map(event -> Event$.MODULE$.invalidEvent(event.eventKey())).collect(Collectors.toList());
        expectedEvents.addAll(events);
        Assert.assertEquals(new HashSet<>(expectedEvents), queue);
    }
}
