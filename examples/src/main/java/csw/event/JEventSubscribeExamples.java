package csw.event;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.MutableBehavior;
import akka.stream.Materializer;
import csw.command.messages.TopLevelActorMessage;
import csw.params.events.Event;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import csw.location.api.models.AkkaLocation;
import csw.params.core.models.Subsystem;
import csw.event.api.javadsl.IEventSubscriber;
import csw.event.api.javadsl.IEventSubscription;
import csw.event.api.scaladsl.SubscriptionModes;
import csw.event.internal.commons.javawrappers.JEventService;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import akka.stream.javadsl.Sink;

public class JEventSubscribeExamples {

    private JEventService eventService;
    private AkkaLocation hcdLocation;
    private Materializer mat;

    public JEventSubscribeExamples(JEventService eventService, AkkaLocation hcdLocation, Materializer mat) {
        this.eventService = eventService;
        this.hcdLocation = hcdLocation;
        this.mat = mat;
    }

    public IEventSubscription callback() {
        //#with-callback

        IEventSubscriber subscriber = eventService.defaultSubscriber();

        EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
        return subscriber.subscribeCallback(Collections.singleton(filterWheelEventKey), event -> { /*do something*/ });

        //#with-callback
    }

    //#with-async-callback
    public IEventSubscription subscribe() {
        IEventSubscriber subscriber = eventService.defaultSubscriber();

        EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
        return subscriber.subscribeAsync(Collections.singleton(filterWheelEventKey), this::callback);
    }

    private CompletableFuture<String> callback(Event event) {
        /* do something */
        return CompletableFuture.completedFuture("some value");
    }
    //#with-async-callback

    //#with-actor-ref
    public IEventSubscription subscribe(ActorContext<TopLevelActorMessage> ctx) {

        IEventSubscriber subscriber = eventService.defaultSubscriber();
        ActorRef<Event> eventHandler = ctx.spawnAnonymous(new JEventHandlerFactory().make());

        EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
        return subscriber.subscribeActorRef(Collections.singleton(filterWheelEventKey), eventHandler);
    }

    public class JEventHandlerFactory {
        public Behavior<Event> make() {
            return Behaviors.setup(JEventHandler::new);
        }
    }

    public class JEventHandler extends MutableBehavior<Event> {
        private ActorContext<Event> ctx;

        JEventHandler(ActorContext<Event> context) {
            ctx = context;
        }

        @Override
        public Behaviors.Receive<Event> createReceive() {
            // handle messages
            return null;
        }
    }
    //#with-actor-ref


    public IEventSubscription source() {
        //#with-source

        IEventSubscriber subscriber = eventService.defaultSubscriber();

        EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
        return subscriber.subscribe(Collections.singleton(filterWheelEventKey)).to(Sink.foreach(event -> { /*do something*/ })).run(mat);

        //#with-source
    }

    public IEventSubscription subscriptionMode() {
        //#with-subscription-mode

        IEventSubscriber subscriber = eventService.defaultSubscriber();

        EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
        return subscriber.subscribeCallback(Collections.singleton(filterWheelEventKey), event -> { /* do something*/ }, Duration.ofMillis(1000), SubscriptionModes.jRateAdapterMode());

        //#with-subscription-mode
    }


    private void subscribeToSubsystemEvents(Subsystem subsystem) {
        // #psubscribe

        IEventSubscriber subscriber = eventService.defaultSubscriber();
        subscriber.pSubscribeCallback(subsystem, "*", event -> { /* do something*/ });

        // #psubscribe
    }

}
