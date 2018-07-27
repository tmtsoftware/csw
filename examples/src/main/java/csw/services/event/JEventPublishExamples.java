package csw.services.event;

import akka.Done;
import akka.actor.Cancellable;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import csw.messages.events.Event;
import csw.messages.events.EventName;
import csw.messages.events.SystemEvent;
import csw.messages.framework.ComponentInfo;
import csw.messages.params.models.Prefix;
import csw.services.event.internal.commons.javawrappers.JEventService;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class JEventPublishExamples {

    private JEventService eventService;
    private Materializer mat;

    public JEventPublishExamples(JEventService eventService, Materializer mat) {
        this.eventService = eventService;
        this.mat = mat;
    }

    public void singleEvent(ComponentInfo componentInfo) {
        //#single-event
        Event event = new SystemEvent(componentInfo.prefix(), new EventName("filter_wheel"));
        eventService.defaultPublisher().thenApply(publisher -> publisher.publish(event));
        //#single-event
    }

    private Event makeEvent(int id, Prefix prefix, EventName name) {
        return new SystemEvent(prefix, name);
    }

    public void source(ComponentInfo componentInfo) {
        int n = 10;

        //#with-source
        eventService.defaultPublisher().thenApply(publisher -> {
            Source<Event, CompletionStage<Done>> eventStream = Source
                    .range(1, n)
                    .map(id -> makeEvent(id, componentInfo.prefix(), new EventName("filter_wheel")))
                    .watchTermination(Keep.right());

            return publisher.<CompletionStage<Done>>publish(eventStream, failure -> { /*do something*/ });

        });
        //#with-source
    }

    //#event-generator
    private CompletableFuture<Cancellable> startPublishingEvents(ComponentInfo componentInfo) {
        Event baseEvent = new SystemEvent(componentInfo.prefix(), new EventName("filter_wheel"));
        return eventService.defaultPublisher().thenApply(publisher -> publisher.publish(() -> eventGenerator(baseEvent), Duration.ofMillis(100)));
    }

    // this holds the logic for event generation, could be based on some computation or current state of HCD
    private Event eventGenerator(Event baseEvent) {
        // add logic here to create a new event and return the same
        return baseEvent;
    }
    //#event-generator
}
