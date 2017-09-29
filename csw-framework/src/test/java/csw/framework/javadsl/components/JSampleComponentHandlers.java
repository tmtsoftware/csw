package csw.framework.javadsl.components;


import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.SampleComponentState;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.models.framework.ComponentInfo;
import csw.messages.*;
import csw.messages.models.ccs.Validations;
import csw.messages.models.ccs.Validation;
import csw.messages.models.ccs.ValidationIssue;
import csw.messages.models.location.TrackingEvent;
import csw.messages.states.CurrentState;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLogger;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;

public class JSampleComponentHandlers extends JComponentHandlers<JComponentDomainMessage> implements JComponentLogger {

    private String componentName;
    // Demonstrating logger accessibility in Java Component handlers
    private ILogger log;

    private ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef;

    private CurrentState currentState = new CurrentState(SampleComponentState.prefix().prefix());

    JSampleComponentHandlers(ActorContext<ComponentMessage> ctx, ComponentInfo componentInfo, ActorRef<PubSub.PublisherMessage<CurrentState>>
            pubSubRef, ILocationService locationService, Class<JComponentDomainMessage> klass) {
        super(ctx, componentInfo, pubSubRef, locationService, klass);
        this.pubSubRef = pubSubRef;
        this.componentName = componentInfo.name();
        this.log = getLogger();
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        log.debug("Initializing Sample component");
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
        return CompletableFuture.supplyAsync(() -> {
            CurrentState initState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.initChoice()));
            PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(initState);

            pubSubRef.tell(publish);
            return BoxedUnit.UNIT;
        });
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public void onDomainMsg(JComponentDomainMessage hcdDomainMsg) {
        CurrentState domainState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.domainChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(domainState);

        pubSubRef.tell(publish);
    }

    @Override
    public Validation onControlCommand(CommandMessage commandMsg) {
        CurrentState commandState;
        if(commandMsg instanceof CommandMessage.Submit) {
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        }
        else {
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
        }

        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(commandState);

        pubSubRef.tell(publish);
        if (commandMsg.command().prefix().prefix().contains("success")) {
            return Validations.JValid();
        } else {
            return new Validations.Invalid(new ValidationIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        }
    }

    @Override
    public CompletableFuture<BoxedUnit> jOnShutdown() {
        return CompletableFuture.supplyAsync(() -> {
        CurrentState shutdownState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.shutdownChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(shutdownState);

        pubSubRef.tell(publish);
        return BoxedUnit.UNIT;
        });
    }

    @Override
    public void onGoOffline() {
        CurrentState offlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.offlineChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(offlineState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onGoOnline() {
        CurrentState onlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.onlineChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(onlineState);

        pubSubRef.tell(publish);
    }

    @Override
    public String componentName() {
        return componentName;
    }
}
