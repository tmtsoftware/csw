package csw.framework.javadsl.components;

import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.ActorMaterializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import csw.common.components.command.ComponentStateForCommand;
import csw.common.components.framework.SampleComponentState;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.scaladsl.CurrentStatePublisher;
import csw.messages.commands.*;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.TrackingEvent;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Parameter;
import csw.messages.params.states.CurrentState;
import csw.messages.params.states.StateName;
import csw.messages.scaladsl.TopLevelActorMessage;
import csw.services.command.scaladsl.CommandResponseManager;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static csw.common.components.command.ComponentStateForCommand.*;
import static csw.messages.commands.CommandResponse.*;

public class JSampleComponentHandlers extends JComponentHandlers {

    // Demonstrating logger accessibility in Java Component handlers
    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private CurrentStatePublisher currentStatePublisher;
    private CurrentState currentState = new CurrentState(SampleComponentState.prefix().prefix(), new StateName("testStateName"));
    private ActorContext<TopLevelActorMessage> actorContext;

    JSampleComponentHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            CommandResponseManager commandResponseManager,
            CurrentStatePublisher currentStatePublisher,
            ILocationService locationService,
            JLoggerFactory loggerFactory
    ) {
        super(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory);
        this.currentStatePublisher = currentStatePublisher;
        this.log = loggerFactory.getLogger(getClass());
        this.commandResponseManager = commandResponseManager;
        this.actorContext = ctx;
    }

    @Override
    public CompletableFuture<Void> jInitialize() {
        log.debug("Initializing Sample component");
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        return CompletableFuture.runAsync(() -> {
            //#currentStatePublisher
            CurrentState initState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.initChoice()));
            currentStatePublisher.publish(initState);
            //#currentStatePublisher
        });
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public void onSubmit(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        currentStatePublisher.publish(submitState);
        processCommand(controlCommand);
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState onewayState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
        currentStatePublisher.publish(onewayState);
        processCommand(controlCommand);
    }

    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        currentStatePublisher.publish(submitState);

        if (controlCommand.commandName().equals(immediateCmd())) {
            return new Completed(controlCommand.runId());
        } else if (controlCommand.commandName().equals(immediateResCmd())) {
            Parameter<Integer> param = JKeyTypes.IntKey().make("encoder").set(22);
            Result result = new Result(controlCommand.source().prefix()).add(param);
            return new CompletedWithResult(controlCommand.runId(), result);
        } else if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            Accepted accepted = new Accepted(controlCommand.runId());
            commandResponseManager.addOrUpdateCommand(controlCommand.runId(), accepted);
            return accepted;
        } else if (controlCommand.commandName().name().contains("failure")) {
            return new Invalid(controlCommand.runId(), new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        } else {
            return new Accepted(controlCommand.runId());
        }
    }

    private void processCommand(ControlCommand controlCommand) {
        publishCurrentState(controlCommand);
        if (controlCommand.commandName().equals(ComponentStateForCommand.matcherCmd()))
            processCommandWithMatcher(controlCommand);
        else if (controlCommand.commandName().equals(ComponentStateForCommand.withoutMatcherCmd()))
            processCommandWithoutMatcher(controlCommand);
        else processCommandWithoutMatcher(controlCommand);

    }

    private void processCommandWithMatcher(ControlCommand controlCommand) {
        Source.range(1, 10)
                .map(i -> {
                    currentStatePublisher.publish(new CurrentState(controlCommand.source().prefix(), new StateName("testStateName")).add(JKeyTypes.IntKey().make("encoder").set(i * 10)));
                    return i;
                })
                .throttle(1, Duration.create(100, TimeUnit.MILLISECONDS), 1, ThrottleMode.shaping())
                .runWith(Sink.ignore(), ActorMaterializer.create(Adapter.toUntyped(actorContext.getSystem())));
    }

    private void processCommandWithoutMatcher(ControlCommand controlCommand) {

        if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
            CompletableFuture<CommandResponse> status = commandResponseManager.jQuery(controlCommand.runId(), Timeout.apply(100, TimeUnit.MILLISECONDS));
            status.thenAccept(response -> {
                if(response instanceof Accepted)
                    commandResponseManager.addOrUpdateCommand(controlCommand.runId(), new CommandResponse.Error(controlCommand.runId(), "Unknown Error occurred"));
            });
        } else {
             commandResponseManager.addOrUpdateCommand(controlCommand.runId(), new Completed(controlCommand.runId()));
        }

    }

    private void publishCurrentState(ControlCommand controlCommand) {
        CurrentState commandState;

        if (controlCommand instanceof Setup)
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice())).add(controlCommand.paramSet().head());
        else
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.observeConfigChoice())).add(controlCommand.paramSet().head());

        // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
        currentStatePublisher.publish(commandState);
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> {
            CurrentState shutdownState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.shutdownChoice()));
            currentStatePublisher.publish(shutdownState);
        });
    }

    @Override
    public void onGoOffline() {
        CurrentState offlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.offlineChoice()));
        currentStatePublisher.publish(offlineState);
    }

    @Override
    public void onGoOnline() {
        CurrentState onlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.onlineChoice()));
        currentStatePublisher.publish(onlineState);
    }
}
