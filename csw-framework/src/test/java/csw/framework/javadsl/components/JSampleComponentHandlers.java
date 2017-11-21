package csw.framework.javadsl.components;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.SampleComponentState;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.ComponentMessage;
import csw.messages.models.PubSub;
import csw.messages.ccs.CommandIssue;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.ControlCommand;
import csw.messages.ccs.commands.Setup;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.TrackingEvent;
import csw.messages.params.states.CurrentState;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JCommonComponentLogger;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;

import static csw.messages.ccs.commands.CommandResponse.*;

public class JSampleComponentHandlers extends JComponentHandlers<JComponentDomainMessage> implements JCommonComponentLogger {

    private String componentName;
    // Demonstrating logger accessibility in Java Component handlers
    private ILogger log;

    private ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef;

    private CurrentState currentState = new CurrentState(SampleComponentState.prefix().prefix());

    JSampleComponentHandlers(
            ActorContext<ComponentMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            Class<JComponentDomainMessage> klass
    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, klass);
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
    public void onSubmit(ControlCommand controlCommand, ActorRef<CommandResponse> actorRef) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(submitState);
        pubSubRef.tell(publish);
        processCommand(controlCommand);
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState onewayState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(onewayState);
        pubSubRef.tell(publish);
        processCommand(controlCommand);
    }

    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(submitState);
        pubSubRef.tell(publish);

        if (controlCommand.prefix().prefix().contains("success")) {
            return new Accepted(controlCommand.runId());
        } else {
            return new Invalid(controlCommand.runId(), new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        }
    }

    private void processCommand(ControlCommand controlCommand) {
        CurrentState commandState;
        if(controlCommand instanceof Setup) {
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice())).add(controlCommand.paramSet().head());
        }
        else {
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.observeConfigChoice())).add(controlCommand.paramSet().head());
        }

        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(commandState);
        pubSubRef.tell(publish);
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
