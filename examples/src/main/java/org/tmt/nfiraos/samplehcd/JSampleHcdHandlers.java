package org.tmt.nfiraos.samplehcd;

import akka.actor.Cancellable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.CurrentStatePublisher;
import csw.messages.commands.*;
import csw.messages.events.Event;
import csw.messages.events.EventName;
import csw.messages.events.SystemEvent;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.TrackingEvent;
import csw.messages.params.generics.JKeyType;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.Id;
import csw.messages.TopLevelActorMessage;
import csw.services.alarm.api.javadsl.IAlarmService;
import csw.services.command.CommandResponseManager;
import csw.services.event.api.javadsl.IEventService;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Domain specific logic should be written in below handlers.
 * These handlers get invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd, these will be first validated in the
 * supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
public class JSampleHcdHandlers extends JComponentHandlers {

    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private CurrentStatePublisher currentStatePublisher;
    private ActorContext<TopLevelActorMessage> actorContext;
    private ILocationService locationService;
    private ComponentInfo componentInfo;
    private IEventService eventService;
    private ActorRef<WorkerCommand> workerActor;


    JSampleHcdHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            CommandResponseManager commandResponseManager,
            CurrentStatePublisher currentStatePublisher,
            ILocationService locationService,
            IEventService eventService,
            IAlarmService alarmService,
            JLoggerFactory loggerFactory
    ) {
        super(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, eventService, alarmService, loggerFactory);
        this.currentStatePublisher = currentStatePublisher;
        this.log = loggerFactory.getLogger(getClass());
        this.commandResponseManager = commandResponseManager;
        this.actorContext = ctx;
        this.locationService = locationService;
        this.componentInfo = componentInfo;
        this.eventService = eventService;
        workerActor = createWorkerActor();
    }

    //#worker-actor
    private interface WorkerCommand {
    }

    private static final class Sleep implements WorkerCommand {
        private final Id runId;
        private final long timeInMillis;

        private Sleep(Id runId, long timeInMillis) {
            this.runId = runId;
            this.timeInMillis = timeInMillis;
        }
    }

    private ActorRef<WorkerCommand> createWorkerActor() {
        return actorContext.spawn(
                Behaviors.receiveMessage(msg -> {
                    if (msg instanceof Sleep) {
                        Sleep sleep = (Sleep) msg;
                        log.trace(() -> "WorkerActor received sleep command with time of " + sleep.timeInMillis + " ms");
                        // simulate long running command
                        Thread.sleep(sleep.timeInMillis);
                        commandResponseManager.addOrUpdateCommand(sleep.runId, new CommandResponse.Completed(sleep.runId));
                    } else {
                        log.error("Unsupported message type");
                    }
                    return Behaviors.same();
                }),
                "WorkerActor"
        );
    }
    //#worker-actor


    //#initialize
    private Optional<Cancellable> maybePublishingGenerator = Optional.empty();
    @Override
    public CompletableFuture<Void> jInitialize() {
        return CompletableFuture.runAsync(() -> {
            log.info("In HCD initialize");
            maybePublishingGenerator = Optional.of(publishCounter());
        });
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        log.debug(() -> "TrackingEvent received: " + trackingEvent.connection().name());
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> log.info("HCD is shutting down"));
    }
    //#initialize

    //#publish
    private int counter = 0;
    private Event incrementCounterEvent() {
        counter += 1;
        Parameter<Integer> param = JKeyType.IntKey().make("counter").set(counter);
        return new SystemEvent(componentInfo.prefix(), new EventName("HcdCounter")).add(param);
    }

    private Cancellable publishCounter() {
        log.info("Starting publish stream.");
        return eventService.defaultPublisher().publish(this::incrementCounterEvent, Duration.ofSeconds(5));
    }

    private void stopPublishingGenerator() {
        log.info("Stopping publish stream");
        maybePublishingGenerator.ifPresent(Cancellable::cancel);
    }
    //#publish

    //#validate
    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        String commandName = controlCommand.commandName().name();
        log.info(() -> "Validating command: " + commandName);
        if (commandName.equals("sleep")) {
            return new CommandResponse.Accepted(controlCommand.runId());
        }
        return new CommandResponse.Invalid(controlCommand.runId(), new CommandIssue.UnsupportedCommandIssue("Command " + commandName + ". not supported."));
    }
    //#validate


    //#onSetup
    @Override
    public void onSubmit(ControlCommand controlCommand) {
        log.info(() -> "Handling command: " + controlCommand.commandName());

        if (controlCommand instanceof Setup) {
            onSetup((Setup) controlCommand);
        } else if (controlCommand instanceof Observe) {
            // implement (or not)
        }
    }

    private void onSetup(Setup setup) {
        Key<Long> sleepTimeKey = JKeyType.LongKey().make("SleepTime");

        // get param from the Parameter Set in the Setup
        Optional<Parameter<Long>> sleepTimeParamOption = setup.jGet(sleepTimeKey);

        // values of parameters are arrays.  Get the first one (the only one in our case) using `head` method available as a convenience method on `Parameter`.
        if (sleepTimeParamOption.isPresent()) {
            Parameter<Long> sleepTimeParam = sleepTimeParamOption.get();
            long sleepTimeInMillis = sleepTimeParam.head();

            log.info(() -> "command payload: " + sleepTimeParam.keyName() + " = " + sleepTimeInMillis);

            workerActor.tell(new Sleep(setup.runId(), sleepTimeInMillis));
        }
    }
    //#onSetup


    @Override
    public void onOneway(ControlCommand controlCommand) {
    }

    @Override
    public void onGoOffline() {
    }

    @Override
    public void onGoOnline() {
    }
}
