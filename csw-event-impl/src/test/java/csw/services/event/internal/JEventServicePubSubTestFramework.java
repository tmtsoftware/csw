package csw.services.event.internal;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.typed.javadsl.Adapter;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.typed.javadsl.TestProbe;
import csw.messages.events.*;
import csw.messages.params.models.Prefix;
import csw.services.event.helpers.Utils;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.IEventSubscription;
import org.junit.Assert;
import scala.Function1;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class JEventServicePubSubTestFramework {

    private IEventPublisher publisher;
    private IEventSubscriber subscriber;
    private Materializer mat;
    private ActorSystem actorSystem;

    private int counter = -1;
    private Cancellable cancellable;

    public JEventServicePubSubTestFramework(IEventPublisher publisher, IEventSubscriber subscriber, ActorSystem actorSystem, Materializer mat) {
        this.publisher = publisher;
        this.subscriber = subscriber;
        this.mat = mat;
        this.actorSystem = actorSystem;
    }

    public void pubsub() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        TestProbe probe = TestProbe.create(Adapter.toTyped(actorSystem));

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        IEventSubscription subscription = subscriber.subscribe(set).take(2).toMat(Sink.foreach(event -> probe.ref().tell(event)), Keep.left()).run(mat);
        Thread.sleep(100);

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        probe.expectMessage(Event$.MODULE$.invalidEvent(eventKey));
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    public void subscribeIndependently() throws InterruptedException, ExecutionException, TimeoutException {
        Prefix prefix = new Prefix("test.prefix");
        EventName eventName1 = new EventName("system1");
        EventName eventName2 = new EventName("system2");
        Event event1 = new SystemEvent(prefix, eventName1);
        Event event2 = new SystemEvent(prefix, eventName2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(event1.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).run(mat);
        pair.first().isReady().get(10, TimeUnit.SECONDS);
        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair2 = subscriber.subscribe(Collections.singleton(event2.eventKey())).take(2).toMat(Sink.seq(), Keep.both()).run(mat);
        pair2.first().isReady().get(10, TimeUnit.SECONDS);
        publisher.publish(event2).get(10, TimeUnit.SECONDS);

        Set<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(event1.eventKey()));
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, new HashSet<>(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS)));

        Set<Event> expectedEvents2 = new HashSet<>();
        expectedEvents2.add(Event$.MODULE$.invalidEvent(event2.eventKey()));
        expectedEvents2.add(event2);

        Assert.assertEquals(expectedEvents2, new HashSet<>(pair2.second().toCompletableFuture().get(10, TimeUnit.SECONDS)));
    }

    public void publishMultiple() throws InterruptedException, TimeoutException, ExecutionException {
        java.util.List<Event> events = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            events.add(Utils.makeEvent(i));
        }

        EventKey eventKey = Utils.makeEvent(0).eventKey();

        List<Event> queue = new ArrayList<>();
        IEventSubscription subscription = subscriber.subscribe(Collections.singleton(eventKey)).toMat(Sink.foreach(queue::add), Keep.left()).run(mat);
        subscription.isReady().get(10, TimeUnit.SECONDS);

        cancellable = publisher.publish(() -> {
            counter += 1;
            if (counter == 10) cancellable.cancel();
            return events.get(counter);
        }, new FiniteDuration(2, TimeUnit.MILLISECONDS));

        Thread.sleep(1000); //TODO : Try to replace with Await

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(11, queue.size());

        events.add(0, Event$.MODULE$.invalidEvent(eventKey));
        Assert.assertEquals(events, queue);
    }

    public void publishMultipleToDifferentChannels() throws InterruptedException, TimeoutException, ExecutionException {
        java.util.List<Event> events = new ArrayList<>();
        for (int i = 101; i < 111; i++) {
            events.add(Utils.makeDistinctEvent(i));
        }

        Set<Event> queue = new HashSet<>();
        IEventSubscription subscription = subscriber.subscribe(events.stream().map(Event::eventKey).collect(Collectors.toSet())).toMat(Sink.foreach(queue::add), Keep.left()).run(mat);
        subscription.isReady().get(10, TimeUnit.SECONDS);

        publisher.publish(Source.fromIterator(events::iterator));

        Thread.sleep(1000);

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(20, queue.size());

        List<Event> expectedEvents = events.stream().map(event -> Event$.MODULE$.invalidEvent(event.eventKey())).collect(Collectors.toList());
        expectedEvents.addAll(events);
        Assert.assertEquals(new HashSet<>(expectedEvents), queue);
    }

    public void retrieveRecentlyPublished() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        Event event2 = Utils.makeEvent(2);
        Event event3 = Utils.makeEvent(3);
        EventKey eventKey = event1.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        publisher.publish(event2).get(10, TimeUnit.SECONDS); // latest event before subscribing

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(eventKey)).take(2).toMat(Sink.seq(), Keep.both()).run(mat);
        pair.first().isReady().get(10, TimeUnit.SECONDS);

        publisher.publish(event3).get(10, TimeUnit.SECONDS);

        java.util.List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(event2);
        expectedEvents.add(event3);

        // assertion against a list ensures that the latest event before subscribing arrives earlier in the stream
        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    public void retrieveInvalidEvent() throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = EventKey.apply(Prefix.apply("test"), EventName.apply("test"));

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(eventKey)).take(1).toMat(Sink.seq(), Keep.both()).run(mat);

        Assert.assertEquals(Collections.singletonList(Event$.MODULE$.invalidEvent(eventKey)), pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    public void retrieveMultipleSubscribedEvents() throws InterruptedException, ExecutionException, TimeoutException {
        Event distinctEvent1 = Utils.makeDistinctEvent(301);
        Event distinctEvent2 = Utils.makeDistinctEvent(302);

        EventKey eventKey1 = distinctEvent1.eventKey();
        EventKey eventKey2 = distinctEvent2.eventKey();

        publisher.publish(distinctEvent1).get(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        Set<EventKey> eventKeys = new HashSet<>();
        eventKeys.add(eventKey1);
        eventKeys.add(eventKey2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(eventKeys).take(2).toMat(Sink.seq(), Keep.both()).run(mat);

        Set<Event> actualEvents = new HashSet<>(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));

        HashSet<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(distinctEvent2.eventKey()));
        expectedEvents.add(distinctEvent1);

        Assert.assertEquals(expectedEvents, actualEvents);
    }

    public void retrieveEventUsingCallback() throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(303);

        TestProbe probe = TestProbe.create(Adapter.toTyped(actorSystem));

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        IEventSubscription subscription = subscriber.subscribeCallback(Collections.singleton(event1.eventKey()), event -> probe.ref().tell(event), mat);
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    public void retrieveEventUsingAsyncCallback() throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(304);

        TestProbe probe = TestProbe.create(Adapter.toTyped(actorSystem));

        Function1<Event, CompletableFuture<?>> asyncCallback = event -> {
            probe.ref().tell(event);
            return CompletableFuture.completedFuture(event);
        };

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        IEventSubscription subscription = subscriber.subscribeAsync(Collections.singleton(event1.eventKey()), asyncCallback, mat);
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));

    }

    public void retrieveEventUsingActorRef() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(305);

        TestProbe probe = TestProbe.create(Adapter.toTyped(actorSystem));

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        IEventSubscription subscription = subscriber.subscribeActorRef(Collections.singleton(event1.eventKey()), probe.ref(), mat);
        probe.expectMessage(event1);

        subscription.unsubscribe().get(10, TimeUnit.SECONDS);

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        probe.expectNoMessage(Duration.ofMillis(200));
    }

    public void get() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        EventKey eventKey = event1.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        Event event = subscriber.get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(event1, event);
    }

    public void retrieveInvalidEventOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = EventKey.apply(Prefix.apply("test"), EventName.apply("test"));
        Event event = subscriber.get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertTrue(((SystemEvent) event).isInvalid());
        Assert.assertEquals(Event$.MODULE$.invalidEvent(eventKey), event);
    }

    public void retrieveEventsForMultipleEventKeysOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(306);
        EventKey eventKey1 = event1.eventKey();

        Event event2 = Utils.makeDistinctEvent(307);
        EventKey eventKey2 = event2.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        HashSet<EventKey> keys = new HashSet<>();
        keys.add(eventKey1);
        keys.add(eventKey2);

        CompletableFuture<Set<Event>> eventsF = subscriber.get(keys);

        HashSet<Event> expectedEvents = new HashSet<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent(eventKey2));
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, eventsF.get(10, TimeUnit.SECONDS));
    }
}
