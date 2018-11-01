package csw.framework.javadsl.components;

import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.ActorMaterializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import csw.common.components.command.ComponentStateForCommand;
import csw.common.components.framework.SampleComponentState;
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.params.commands.*;
import csw.location.api.models.TrackingEvent;
import csw.params.core.models.Id;
import csw.params.javadsl.JKeyType;
import csw.params.core.generics.Parameter;
import csw.params.core.states.CurrentState;
import csw.params.core.states.StateName;
import csw.command.client.CommandResponseManager;
import csw.logging.javadsl.ILogger;

import java.time.Duration;
import java.util.concurrent.*;

import static csw.common.components.command.ComponentStateForCommand.*;

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
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        currentStatePublisher.publish(submitState);
        return processSubmitCommand(controlCommand);
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {
        // Adding item from CommandMessage paramset to ensure things are working
        CurrentState onewayState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.oneWayCommandChoice()));
        currentStatePublisher.publish(onewayState);
        processOnewayCommand(controlCommand);
    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        CurrentState submitState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        currentStatePublisher.publish(submitState);

        // Special case to accept failure after validation
        if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            return new CommandResponse.Accepted(controlCommand.runId());
        } else
        if (controlCommand.commandName().name().contains("failure")) {
            return new CommandResponse.Invalid(controlCommand.runId(), new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        } else {
            return new CommandResponse.Accepted(controlCommand.runId());
        }
    }

    private CommandResponse.SubmitResponse processSubmitCommand(ControlCommand controlCommand) {
        publishCurrentState(controlCommand);
        if (controlCommand.commandName().equals(immediateCmd())) {
            return new CommandResponse.Completed(controlCommand.runId());
        } else if (controlCommand.commandName().equals(immediateResCmd())) {
            Parameter<Integer> param = JKeyType.IntKey().make("encoder").set(22);
            Result result = new Result(controlCommand.source().prefix()).add(param);
            return new CommandResponse.CompletedWithResult(controlCommand.runId(), result);
        } else if (controlCommand.commandName().equals(ComponentStateForCommand.matcherCmd())) {
            processCommandWithMatcher(controlCommand);
            return new CommandResponse.Started(controlCommand.runId());
        } else if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            return processCommandWithoutMatcher(controlCommand);
        } else if (controlCommand.commandName().equals(ComponentStateForCommand.withoutMatcherCmd())) {
            return processCommandWithoutMatcher(controlCommand);
        }

        return new CommandResponse.Completed(controlCommand.runId());
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
                    currentStatePublisher.publish(new CurrentState(controlCommand.source().prefix(), new StateName("testStateName")).add(JKeyType.IntKey().make("encoder").set(i * 10)));
                    return i;
                })
                .throttle(1, Duration.ofMillis(100), 1, ThrottleMode.shaping())
                .runWith(Sink.ignore(), ActorMaterializer.create(Adapter.toUntyped(actorContext.getSystem())));
    }

    private void sendCRM(Id runId, CommandResponse.SubmitResponse response) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                commandResponseManager.addOrUpdateCommand(response);
            }
        };
        // Wait a bit and then set CRM to Completed
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ((ScheduledExecutorService) executor).schedule(task, 1, TimeUnit.SECONDS);
    }



    private CommandResponse.SubmitResponse processCommandWithoutMatcher(ControlCommand controlCommand) {

        if (controlCommand.commandName().equals(failureAfterValidationCmd())) {
            // Set CRM to Error after 1 second
            sendCRM(controlCommand.runId(), new CommandResponse.Error(controlCommand.runId(), "Unknown Error occurred"));
            return new CommandResponse.Started(controlCommand.runId());

            /*  Retained for checking 371
            // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
            CompletableFuture<CommandResponse.QueryResponse> status = commandResponseManager.jQuery(controlCommand.runId(), Timeout.apply(100, TimeUnit.MILLISECONDS));
            status.thenAccept(response -> {
                if(response instanceof CommandResponse.Started) {
                    //commandResponseManager.addOrUpdateCommand(controlCommand.runId(), new CommandResponse.Error(controlCommand.runId(), "Unknown Error occurred"));
                    //return new CommandResponse.Error(controlCommand.runId(), "Unknown Error occurred");
                                }
                return new CommandResponse.Error(controlCommand.runId(), "Unknown Error occurred");
            });
            */
        } else {
            // Set CRM to Completed after 1 second
            sendCRM(controlCommand.runId(),  new CommandResponse.Completed(controlCommand.runId()));
            return new CommandResponse.Started(controlCommand.runId());
        }

    }

    private void publishCurrentState(ControlCommand controlCommand) {
        CurrentState commandState;

        if (controlCommand instanceof Setup) {
            commandState = new CurrentState(SampleComponentState.prefix().prefix(), new StateName("testStateSetup")).add(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice())).add(controlCommand.paramSet().head());
        } else
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
