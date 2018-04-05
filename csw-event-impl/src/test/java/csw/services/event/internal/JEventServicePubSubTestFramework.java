package csw.services.event.internal;

import akka.actor.Cancellable;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import csw.messages.events.*;
import csw.messages.params.models.Prefix;
import csw.services.event.helpers.Utils;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.IEventSubscription;
import org.junit.Assert;
import scala.concurrent.duration.FiniteDuration;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class JEventServicePubSubTestFramework {

    private IEventPublisher publisher;
    private IEventSubscriber subscriber;
    private Materializer mat;

    private int counter = -1;
    private Cancellable cancellable;

    public JEventServicePubSubTestFramework(IEventPublisher publisher, IEventSubscriber subscriber, Materializer mat) {
        this.publisher = publisher;
        this.subscriber = subscriber;
        this.mat = mat;
    }

    public void pubsub() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(set).toMat(Sink.seq(), Keep.both()).run(mat);
        Thread.sleep(2000);

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent());
        expectedEvents.add(event1);

        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get());
    }

    public void subscribeIndependently() throws InterruptedException, ExecutionException, TimeoutException {
        Prefix prefix = new Prefix("test.prefix");
        EventName eventName1 = new EventName("system1");
        EventName eventName2 = new EventName("system2");
        Event event1 = new SystemEvent(prefix, eventName1);
        Event event2 = new SystemEvent(prefix, eventName2);

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(event1.eventKey());

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(set).toMat(Sink.seq(), Keep.both()).run(mat);
        Thread.sleep(1000);
        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        java.util.Set<EventKey> set2 = new HashSet<>();
        set2.add(event2.eventKey());

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair2 = subscriber.subscribe(set2).toMat(Sink.seq(), Keep.both()).run(mat);
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
    
    public void publishMultiple() throws InterruptedException {
        java.util.List<Event> events = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            events.add(Utils.makeEvent(i));
        }

        Set<EventKey> eventKeys = new HashSet<>();
        eventKeys.add(Utils.makeEvent(0).eventKey());

        List<Event> queue = new ArrayList<>();
        subscriber.subscribe(eventKeys).runForeach(queue::add, mat);

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
    
    public void publishMultipleToDifferentChannels() throws InterruptedException {
        java.util.List<Event> events = new ArrayList<>();
        for (int i = 101; i < 111; i++) {
            events.add(Utils.makeDistinctEvent(i));
        }

        Set<Event> queue = new HashSet<>();
        subscriber.subscribe(events.stream().map(Event::eventKey).collect(Collectors.toSet())).runForeach(queue::add, mat);

        Thread.sleep(500);

        publisher.publish(Source.fromIterator(events::iterator));

        Thread.sleep(1000);

        // subscriber will receive an invalid event first as subscription happened before publishing started.
        // The 10 published events will follow
        Assert.assertEquals(11, queue.size());

        events.add(0, Event$.MODULE$.invalidEvent());
        Assert.assertEquals(new HashSet<>(events), queue);
    }
    
    public void retrieveRecentlyPublished() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        Event event2 = Utils.makeEvent(2);
        Event event3 = Utils.makeEvent(3);
        EventKey eventKey = event1.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);
        publisher.publish(event2).get(10, TimeUnit.SECONDS);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(eventKey)).toMat(Sink.seq(), Keep.both()).run(mat);
        Thread.sleep(1000);

        publisher.publish(event3).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        java.util.List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(event2);
        expectedEvents.add(event3);

        Assert.assertEquals(expectedEvents, pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }
    
    public void retrieveInvalidEvent() throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = new EventKey("test");

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(Collections.singleton(eventKey)).toMat(Sink.seq(), Keep.both()).run(mat);
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        Assert.assertEquals(Collections.singletonList(Event$.MODULE$.invalidEvent()), pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    public void retrieveMultipleSubscribedEvents() throws InterruptedException, ExecutionException, TimeoutException {
        Event distinctEvent1 = Utils.makeDistinctEvent(201);
        Event distinctEvent2 = Utils.makeDistinctEvent(202);

        EventKey eventKey1 = distinctEvent1.eventKey();
        EventKey eventKey2 = distinctEvent2.eventKey();

        publisher.publish(distinctEvent1).get(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        Set<EventKey> eventKeys = new HashSet<>();
        eventKeys.add(eventKey1);
        eventKeys.add(eventKey2);

        Pair<IEventSubscription, CompletionStage<List<Event>>> pair = subscriber.subscribe(eventKeys).toMat(Sink.seq(), Keep.both()).run(mat);
        Thread.sleep(1000);

        pair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        Set<Event> actualEvents = new HashSet<>(pair.second().toCompletableFuture().get(10, TimeUnit.SECONDS));
        Assert.assertEquals(Collections.singleton(distinctEvent1), actualEvents);
    }

    public void get() throws InterruptedException, ExecutionException, TimeoutException {
        Event event1 = Utils.makeEvent(1);
        EventKey eventKey = event1.eventKey();

        publisher.publish(event1).get(10, TimeUnit.SECONDS);

        Event event = subscriber.get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(event1, event);
    }

    public void retrieveInvalidEventOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        EventKey eventKey = new EventKey("test");
        Event event = subscriber.get(eventKey).get(10, TimeUnit.SECONDS);

        Assert.assertEquals(Event$.MODULE$.invalidEvent(), event);
    }
}
