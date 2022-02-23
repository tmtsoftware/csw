package csw.framework.javadsl.components;

import akka.actor.Cancellable;
import akka.actor.typed.javadsl.ActorContext;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import csw.command.client.CommandResponseManager;
import csw.command.client.messages.TopLevelActorMessage;
import csw.common.components.command.ComponentStateForCommand;
import csw.common.components.framework.SampleComponentState;
import csw.event.api.javadsl.IEventService;
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.*;
import csw.params.commands.CommandResponse.Completed;
import csw.params.commands.CommandResponse.Started;
import csw.params.commands.CommandResponse.SubmitResponse;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.core.states.CurrentState;
import csw.params.core.states.StateName;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.time.core.models.UTCTime;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static csw.common.components.command.ComponentStateForCommand.*;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class JSampleComponentHandlers extends JComponentHandlers {

    // Demonstrating logger accessibility in Java Component handlers
    private final ILogger log;
    private final CommandResponseManager commandResponseManager;
    private final CurrentStatePublisher currentStatePublisher;
    private final CurrentState currentState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateName"));
    private final ActorContext<TopLevelActorMessage> actorContext;
    private final IEventService eventService;
    private Optional<Cancellable> diagModeCancellable = Optional.empty();

    JSampleComponentHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.currentStatePublisher = cswCtx.currentStatePublisher();
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.commandResponseManager = cswCtx.commandResponseManager();
        this.actorContext = ctx;
        this.eventService = cswCtx.eventService();
    }

    @Override
    public void initialize() {
        log.debug("Initializing Sample component");
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        //#currentStatePublisher
        CurrentState initState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.initChoice()));
        currentStatePublisher.publish(initState);
        //#currentStatePublisher
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        if (controlCommand.commandName().equals(hcdCurrentStateCmd())) {
            // This is special because test doesn't want these other CurrentState values published
            return new CommandResponse.Accepted(runId);
        } else if (controlCommand.commandName().equals(crmAddOrUpdateCmd())) {
            return new CommandResponse.Accepted(runId);
        } else {
            // All other tests
            CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
            currentStatePublisher.publish(submitState);

            // Special case to accept failure after validation
            if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
                return new CommandResponse.Accepted(runId);
            } else if (controlCommand.commandName().name().contains("failure")) {
                // invalidCmd comes here
                return new CommandResponse.Invalid(runId, new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
            } else {
                return new CommandResponse.Accepted(runId);
            }
        }
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        if (controlCommand.commandName().equals(crmAddOrUpdateCmd())) {
            return crmAddOrUpdate((Setup) controlCommand, runId);
        } else {
            CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
            currentStatePublisher.publish(submitState);
            return processSubmitCommand(runId, controlCommand);
        }
    }

    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {
        if (controlCommand.commandName().equals(hcdCurrentStateCmd())) {
            // Special handling for oneway to test current state
            processCurrentStateOnewayCommand((Setup) controlCommand);
        } else {
            // Adding item from CommandMessage paramset to ensure things are working
            CurrentState onewayState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
            currentStatePublisher.publish(onewayState);
            processOnewayCommand(controlCommand);
        }
    }

    private CommandResponse.SubmitResponse processSubmitCommand(Id runId, ControlCommand controlCommand) {
        publishCurrentState(controlCommand);
        if (controlCommand.commandName().equals(immediateCmd())) {
            return new CommandResponse.Completed(runId);
        } else if (controlCommand.commandName().equals(immediateResCmd())) {
            // Copy the input paramset to the output
            Result result = new Result().jMadd(controlCommand.paramType().jParamSet());
            return new CommandResponse.Completed(runId, result);
        } else if (controlCommand.commandName().equals(ComponentStateForCommand.matcherCmd())) {
            processCommandWithMatcher(controlCommand);
            return new CommandResponse.Started(runId);
        } else if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            return processCommandWithoutMatcher(runId, controlCommand);
        } else if (controlCommand.commandName().equals(ComponentStateForCommand.longRunningCmd())) {
            return processCommandWithoutMatcher(runId, controlCommand);
        }

        return new CommandResponse.Completed(runId);
    }

    //#updateCommand
    private CommandResponse.SubmitResponse crmAddOrUpdate(Setup setup, Id runId) {
        // This simulates some worker task doing something that finishes after onSubmit returns
        Runnable task = () -> commandResponseManager.updateCommand(new Completed(runId));

        // Wait a bit and then set CRM to Completed
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(task, 1, TimeUnit.SECONDS);

        // Return Started from onSubmit
        return new Started(runId);
    }
    //#updateCommand

    private void processCurrentStateOnewayCommand(Setup setup) {
        //#subscribeCurrentState
        Key<Integer> encoder = JKeyType.IntKey().make("encoder", JUnits.encoder);
        int expectedEncoderValue = setup.jGet(encoder).orElseThrow().head();

        CurrentState currentState = new CurrentState(prefix(), new StateName("HCDState")).add(encoder().set(expectedEncoderValue));
        currentStatePublisher.publish(currentState);
        //#subscribeCurrentState
    }

    private void processOnewayCommand(ControlCommand controlCommand) {
        publishCurrentState(controlCommand);
        if (controlCommand.commandName().equals(ComponentStateForCommand.matcherCmd())) {
            processCommandWithMatcher(controlCommand);
        }
        // Nothing else done in oneway
    }


    private void processCommandWithMatcher(ControlCommand controlCommand) {
        Source.range(1, 10)
                .map(i -> {
                    currentStatePublisher.publish(new CurrentState(controlCommand.source(), new StateName("testStateName")).add(JKeyType.IntKey().make("encoder", JUnits.encoder).set(i * 10)));
                    return i;
                })
                .throttle(1, Duration.ofMillis(100), 1, (ThrottleMode) ThrottleMode.shaping())
                .runWith(Sink.ignore(), actorContext.getSystem());
    }


    private SubmitResponse processCommandWithoutMatcher(Id runId, ControlCommand controlCommand) {
        if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            // Set CRM to Error after 1 second
            sendCRM(1, new CommandResponse.Error(runId, "Unknown Error occurred"));
        } else {
            Parameter<Integer> parameter = JKeyType.IntKey().make("encoder", JUnits.encoder).set(20);
            Result result = new Result().add(parameter);

            // Returns Started and completes through CRM after 1 second
            sendCRM(1, new CommandResponse.Completed(runId, result));
        }
        return new Started(runId);
    }

    private void parameterDelay(Id runId, Setup setup) {
        Key<Integer> encoder = JKeyType.IntKey().make("delay", JUnits.second);
        int delay = setup.jGet(encoder).orElseThrow().head();
        sendCRM(delay, new CommandResponse.Completed(runId));
    }

    // This test routine just delays before updating CRM - delay is always in seconds
    private void sendCRM(long delay, CommandResponse.SubmitResponse response) {
        Runnable task = () -> commandResponseManager.updateCommand(response);
        // Wait a bit and then set CRM to response
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(task, delay, TimeUnit.SECONDS);
    }

    private void publishCurrentState(ControlCommand controlCommand) {
        CurrentState commandState;

        if (controlCommand instanceof Setup) {
            commandState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateSetup"))
                    .add(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice())).add(controlCommand.paramSet().head());
        } else
            commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.observeConfigChoice())).add(controlCommand.paramSet().head());

        // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
        currentStatePublisher.publish(commandState);
    }

    @Override
    public void onShutdown() {
        CurrentState shutdownState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.shutdownChoice()));
        currentStatePublisher.publish(shutdownState);
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

    //#onDiagnostic-mode
    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
        if (hint.equals("engineering")) {
            var event = new SystemEvent(Prefix.apply(JSubsystem.TCS, "prefix"), new EventName("eventName"))
                    .add(JKeyType.IntKey().make("diagnostic-data", JUnits.NoUnits).set(1));
            diagModeCancellable.map(Cancellable::cancel); // cancel previous diagnostic publishing
            diagModeCancellable = Optional.of(
                    eventService.defaultPublisher().publish(
                            () -> Optional.of(event),
                            startTime,
                            Duration.ofMillis(200)
                    )

            );
        }
        // other supported diagnostic modes go here
    }
    //#onDiagnostic-mode

    //#onOperations-mode
    @Override
    public void onOperationsMode() {
        diagModeCancellable.map(Cancellable::cancel); // cancel diagnostic mode
    }
    //#onOperations-mode
}
