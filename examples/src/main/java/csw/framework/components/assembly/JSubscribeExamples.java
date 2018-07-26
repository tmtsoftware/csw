package csw.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.stream.Materializer;
import csw.messages.TopLevelActorMessage;
import csw.messages.events.Event;
import csw.messages.events.EventKey;
import csw.messages.events.EventName;
import csw.messages.location.AkkaLocation;
import csw.messages.params.models.Subsystem;
import csw.services.event.api.javadsl.IEventSubscriber;
import csw.services.event.api.scaladsl.SubscriptionModes;
import csw.services.event.internal.commons.javawrappers.JEventService;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class JSubscribeExamples {

    private JEventService eventService;
    private Materializer mat;

    public JSubscribeExamples(JEventService eventService, Materializer mat){
        this.eventService = eventService;
        this.mat = mat;
    }

    public void callback(AkkaLocation hcdLocation) {
        //#with-callback

        CompletableFuture<IEventSubscriber> subscriberF = eventService.defaultSubscriber();

        subscriberF.thenApply(subscriber -> {
            EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
            return subscriber.subscribeCallback(Collections.singleton(filterWheelEventKey), event -> { /*do something*/ });
        });

        //#with-callback
    }

    public void asyncCallback(AkkaLocation hcdLocation) {
        //#with-async-callback

        CompletableFuture<IEventSubscriber> subscriberF = eventService.defaultSubscriber();

        subscriberF.thenApply(subscriber -> {
            EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
            return subscriber.subscribeAsync(Collections.singleton(filterWheelEventKey), CompletableFuture::completedFuture);
        });

        //#with-async-callback
    }

    public void actorRef(AkkaLocation hcdLocation, akka.actor.typed.javadsl.ActorContext<TopLevelActorMessage> ctx) {
        //#with-actor-ref

        CompletableFuture<IEventSubscriber> subscriberF = eventService.defaultSubscriber();
        ActorRef<Event> eventHandler = ctx.spawnAnonymous(JEventHandlerFactory.make());

        subscriberF.thenApply(subscriber -> {
            EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
            return subscriber.subscribeActorRef(Collections.singleton(filterWheelEventKey), eventHandler);
        });

        //#with-actor-ref
    }

    public void source(AkkaLocation hcdLocation) {
        //#with-source

        CompletableFuture<IEventSubscriber> subscriberF = eventService.defaultSubscriber();

        subscriberF.thenApply(subscriber -> {
            EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
            return subscriber.subscribe(Collections.singleton(filterWheelEventKey)).runForeach(event -> { /*do something*/ }, mat);
        });

        //#with-source
    }

    public void subscriptionMode(AkkaLocation hcdLocation) {
        //#with-subscription-mode

        CompletableFuture<IEventSubscriber> subscriberF = eventService.defaultSubscriber();

        subscriberF.thenApply(subscriber -> {
            EventKey filterWheelEventKey = new EventKey(hcdLocation.prefix(), new EventName("filter_wheel"));
            return subscriber.subscribeCallback(Collections.singleton(filterWheelEventKey), event -> { /* do something*/ },  Duration.ofMillis(1000), SubscriptionModes.jRateAdapterMode());
        });

        //#with-subscription-mode
    }

    // #psubscribe
    private void subscribeToSubsystemEvents(Subsystem subsystem)  {
        CompletableFuture<IEventSubscriber> subscriberF = eventService.defaultSubscriber();

        subscriberF.thenApply(subscriber -> subscriber.pSubscribeCallback(subsystem, "*", this::callback));
    }
    // #psubscribe

    private void callback(Event event){
        //do something
    }

}
