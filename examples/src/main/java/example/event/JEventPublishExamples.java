package example.event;

import akka.Done;
import akka.actor.Cancellable;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import csw.params.events.Event;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.command.client.models.framework.ComponentInfo;
import csw.event.client.internal.commons.javawrappers.JEventService;
import csw.prefix.models.Prefix;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class JEventPublishExamples {

    private final JEventService eventService;

    public JEventPublishExamples(JEventService eventService) {
        this.eventService = eventService;
    }

    public void singleEvent(ComponentInfo componentInfo) {
        //#single-event
        Event event = new SystemEvent(componentInfo.prefix(), new EventName("filter_wheel"));
        eventService.defaultPublisher().publish(event);
        //#single-event
    }

    private Event makeEvent(int id, Prefix prefix, EventName name) {
        return new SystemEvent(prefix, name);
    }

    public CompletionStage<Done> source(ComponentInfo componentInfo) {
        int n = 10;

        //#with-source
        Source<Event, CompletionStage<Done>> eventStream = Source
                .range(1, n)
                .map(id -> makeEvent(id, componentInfo.prefix(), new EventName("filter_wheel")))
                .watchTermination(Keep.right());

        return eventService.defaultPublisher().<CompletionStage<Done>>publish(eventStream, failure -> { /*do something*/ });
        //#with-source
    }

    //#event-generator
    private Cancellable startPublishingEvents(ComponentInfo componentInfo) {
        Event baseEvent = new SystemEvent(componentInfo.prefix(), new EventName("filter_wheel"));
        return eventService.defaultPublisher().publish(() -> eventGenerator(baseEvent), Duration.ofMillis(100));
    }

    // this holds the logic for event generation, could be based on some computation or current state of HCD
    private Optional<Event> eventGenerator(Event baseEvent) {
        // add logic here to create a new event and return the same
        return Optional.ofNullable(baseEvent);
    }
    //#event-generator
}
