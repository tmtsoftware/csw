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
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.command.messages.TopLevelActorMessage;
import csw.params.commands.*;
import csw.location.api.models.TrackingEvent;
import csw.params.javadsl.JKeyType;
import csw.params.core.generics.Parameter;
import csw.params.core.states.CurrentState;
import csw.params.core.states.StateName;
import csw.command.CommandResponseManager;
import csw.logging.javadsl.ILogger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static csw.common.components.command.ComponentStateForCommand.*;
import static csw.messages.commands.CommandResponse.Completed;
import static csw.messages.commands.CommandResponse.CompletedWithResult;
import static csw.params.commands.CommandResponse.*;

public class JSampleComponentHandlers extends JComponentHandlers {

    // Demonstrating logger accessibility in Java Component handlers
    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private CurrentStatePublisher currentStatePublisher;
    private CurrentState currentState = new CurrentState(SampleComponentState.prefix().prefix(), new StateName("testStateName"));
    private ActorContext<TopLevelActorMessage> actorContext;

    JSampleComponentHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.currentStatePublisher = cswCtx.currentStatePublisher();
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.commandResponseManager = cswCtx.commandResponseManager();
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
    /// TODO - probbably need to make this work when running tests
    public Responses.SubmitResponse onSubmit(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        currentStatePublisher.publish(submitState);
        processCommand(controlCommand);
        return new Responses.Completed(controlCommand.runId());
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState onewayState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
        currentStatePublisher.publish(onewayState);
        processCommand(controlCommand);
    }

    @Override
    public Responses.ValidationResponse validateCommand(ControlCommand controlCommand) {
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        currentStatePublisher.publish(submitState);
// TODO -- need to fix this by moving some to process command with submit
        /*
        if (controlCommand.commandName().equals(immediateCmd())) {
            return new Completed(controlCommand.runId());
        } else if (controlCommand.commandName().equals(immediateResCmd())) {
            Parameter<Integer> param = JKeyTypes.IntKey().make("encoder").set(22);
            Result result = new Result(controlCommand.source().prefix()).add(param);
            return new CompletedWithResult(controlCommand.runId(), result);
        } else if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            ValidationResponse.Accepted accepted = new ValidationResponse.Accepted(controlCommand.runId());
            commandResponseManager.addOrUpdateCommand(controlCommand.runId(), accepted);
            return accepted;

        } else */ if (controlCommand.commandName().name().contains("failure")) {
            return new Responses.Invalid(controlCommand.runId(), new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        } else {
            return new Responses.Accepted(controlCommand.runId());
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
                .throttle(1, Duration.ofMillis(100), 1, ThrottleMode.shaping())
                .runWith(Sink.ignore(), ActorMaterializer.create(Adapter.toUntyped(actorContext.getSystem())));
    }

    private void processCommandWithoutMatcher(ControlCommand controlCommand) {

        if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
            CompletableFuture<Responses.SubmitResponse> status = commandResponseManager.jQuery(controlCommand.runId(), Timeout.apply(100, TimeUnit.MILLISECONDS));
            status.thenAccept(response -> {
                if(response instanceof Responses.Invalid)
                    commandResponseManager.addOrUpdateCommand(controlCommand.runId(), new Responses.Error(controlCommand.runId(), "Unknown Error occurred"));
            });
        } else {
             commandResponseManager.addOrUpdateCommand(controlCommand.runId(), new Responses.Completed(controlCommand.runId()));
        }

    }

    private void publishCurrentState(ControlCommand controlCommand) {
        CurrentState commandState;

        if (controlCommand instanceof Setup)
            commandState = new CurrentState(SampleComponentState.prefix().prefix(), new StateName("testStateSetup")).add(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice())).add(controlCommand.paramSet().head());
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
