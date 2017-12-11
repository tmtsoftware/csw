package csw.framework.javadsl.components;

import akka.stream.ActorMaterializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.command.ComponentStateForCommand;
import csw.common.components.framework.SampleComponentState;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.TopLevelActorMessage;
import csw.messages.ccs.CommandIssue;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.ControlCommand;
import csw.messages.ccs.commands.Setup;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.TrackingEvent;
import csw.messages.models.PubSub;
import csw.messages.models.PubSub.Publish;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.states.CurrentState;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand;
import static csw.messages.ccs.commands.CommandResponse.*;

public class JSampleComponentHandlers extends JComponentHandlers<JTopLevelActorDomainMessage> {

    // Demonstrating logger accessibility in Java Component handlers
    private ILogger log;
    private ActorRef<CommandResponseManagerMessage> commandResponseManagerRef;
    private ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef;
    private CurrentState currentState = new CurrentState(SampleComponentState.prefix().prefix());
    private ActorContext<TopLevelActorMessage> actorContext;

    JSampleComponentHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory,
            Class<JTopLevelActorDomainMessage> klass
    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory, klass);
        this.pubSubRef = pubSubRef;
        this.log = loggerFactory.getLogger(getClass());
        this.commandResponseManagerRef = commandResponseManager;
        this.actorContext = ctx;
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        log.debug("Initializing Sample component");
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
        return CompletableFuture.supplyAsync(() -> {
            CurrentState initState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.initChoice()));
            Publish<CurrentState> publish = new Publish<>(initState);
            pubSubRef.tell(publish);
            return BoxedUnit.UNIT;
        });
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public void onDomainMsg(JTopLevelActorDomainMessage hcdDomainMsg) {
        CurrentState domainState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.domainChoice()));
        Publish<CurrentState> publish = new Publish<>(domainState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onSubmit(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        Publish<CurrentState> publish = new Publish<>(submitState);
        pubSubRef.tell(publish);
        processCommand(controlCommand);
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState onewayState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
        Publish<CurrentState> publish = new Publish<>(onewayState);
        pubSubRef.tell(publish);
        processCommand(controlCommand);
    }

    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        Publish<CurrentState> publish = new Publish<>(submitState);
        pubSubRef.tell(publish);

        if (controlCommand.target().prefix().contains("failure")) {
            return new Invalid(controlCommand.runId(), new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        } else {
            return new Accepted(controlCommand.runId());
        }
    }

    private void processCommand(ControlCommand controlCommand) {
        publishCurrentState(controlCommand);
        if(controlCommand.target().equals(ComponentStateForCommand.matcherPrefix()))
            processCommandWithMatcher(controlCommand);
        if(controlCommand.target().equals(ComponentStateForCommand.withoutMatcherPrefix()))
            processCommandWithoutMatcher(controlCommand);
    }

    private void processCommandWithMatcher(ControlCommand controlCommand) {
        Source.range(1, 10)
                .map(i -> {pubSubRef.tell(new Publish(new CurrentState(controlCommand.target().prefix()).add(JKeyTypes.IntKey().make("encoder").set(i * 10)))); return i;})
                .throttle(1, Duration.create(100, TimeUnit.MILLISECONDS), 1, ThrottleMode.shaping())
                .runWith(Sink.ignore(), ActorMaterializer.create(akka.typed.javadsl.Adapter.toUntyped(actorContext.getSystem())));
    }

    private void processCommandWithoutMatcher(ControlCommand controlCommand) {
        CommandResponseManagerMessage updateCommand = new AddOrUpdateCommand(controlCommand.runId(), new Completed(controlCommand.runId()));
        commandResponseManagerRef.tell(updateCommand);
    }

    private void publishCurrentState(ControlCommand controlCommand) {
        CurrentState commandState;

        if(controlCommand instanceof Setup)
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice())).add(controlCommand.paramSet().head());
        else
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.observeConfigChoice())).add(controlCommand.paramSet().head());

        Publish<CurrentState> publish = new Publish<>(commandState);
        pubSubRef.tell(publish);
    }

    @Override
    public CompletableFuture<BoxedUnit> jOnShutdown() {
        return CompletableFuture.supplyAsync(() -> {
        CurrentState shutdownState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.shutdownChoice()));
        Publish<CurrentState> publish = new Publish<>(shutdownState);

        pubSubRef.tell(publish);
        return BoxedUnit.UNIT;
        });
    }

    @Override
    public void onGoOffline() {
        CurrentState offlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.offlineChoice()));
        Publish<CurrentState> publish = new Publish<>(offlineState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onGoOnline() {
        CurrentState onlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.onlineChoice()));
        Publish<CurrentState> publish = new Publish<>(onlineState);

        pubSubRef.tell(publish);
    }
}
