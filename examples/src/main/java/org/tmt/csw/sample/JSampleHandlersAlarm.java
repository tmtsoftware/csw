package org.tmt.csw.sample;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.util.Timeout;
import csw.alarm.api.javadsl.JAlarmSeverity;
import csw.alarm.models.AlarmSeverity;
import csw.alarm.models.Key.AlarmKey;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.command.client.messages.TopLevelActorMessage;
import csw.event.api.javadsl.IEventSubscription;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.*;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandName;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.core.models.ObsId;
import csw.params.events.Event;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.models.Prefix;
import csw.time.core.models.UTCTime;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
public class JSampleHandlersAlarm extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;
    private final ActorContext<TopLevelActorMessage> actorContext;
    private final ActorRef<WorkerCommand> commandSender;

    JSampleHandlersAlarm(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.actorContext = ctx;
        this.commandSender = createWorkerActor();
    }

    //#worker-actor
    private interface WorkerCommand {
    }

    private static final class SendCommand implements WorkerCommand {
        private final ICommandService hcd;

        private SendCommand(ICommandService hcd) {
            this.hcd = hcd;
        }
    }

    private ActorRef<WorkerCommand> createWorkerActor() {
        return actorContext.spawn(
                Behaviors.receiveMessage(msg -> {
                    if (msg instanceof SendCommand) {
                        SendCommand command = (SendCommand) msg;
                        log.trace("WorkerActor received SendCommand message.");
                        handle(command.hcd);
                    } else {
                        log.error("Unsupported messsage type");
                    }
                    return Behaviors.same();
                }),
                "CommandSender"
        );
    }

    private void handle(ICommandService hcd) {

        // Construct Setup command
        Key<Long> sleepTimeKey = JKeyType.LongKey().make("SleepTime", JUnits.millisecond);
        Parameter<Long> sleepTimeParam = sleepTimeKey.set(5000L);

        Setup setupCommand = new Setup(cswCtx.componentInfo().prefix(), new CommandName("sleep"), Optional.of(ObsId.apply("2018A-P001-O123"))).add(sleepTimeParam);

        Timeout submitTimeout = new Timeout(1, TimeUnit.SECONDS);
        Timeout commandResponseTimeout = new Timeout(10, TimeUnit.SECONDS);

        // Submit command, and handle validation response. Final response is returned as a Future
        CompletableFuture<CommandResponse.SubmitResponse> submitCommandResponseF = hcd.submitAndWait(setupCommand, submitTimeout)
                .thenCompose(commandResponse -> {
                    if (!(commandResponse instanceof CommandResponse.Invalid || commandResponse instanceof CommandResponse.Locked)) {
                        return CompletableFuture.completedFuture(commandResponse);
                    } else {
                        log.error("Sleep command invalid");
                        return CompletableFuture.completedFuture(new CommandResponse.Error(commandResponse.runId(), "test error"));
                    }
                });


        // Wait for final response, and log result
        submitCommandResponseF.toCompletableFuture().thenAccept(commandResponse -> {
            if (commandResponse instanceof CommandResponse.Completed) {
                log.info("Command completed successfully");
            } else if (commandResponse instanceof CommandResponse.Error) {
                CommandResponse.Error x = (CommandResponse.Error) commandResponse;
                log.error(() -> "Command Completed with error: " + x.message());
            } else {
                log.error("Command failed");
            }
        });
    }
    //#worker-actor

    //#initialize
    private Optional<IEventSubscription> maybeEventSubscription = Optional.empty();

    @Override
    public void initialize() {
        log.info("In Assembly initialize");
        maybeEventSubscription = Optional.of(subscribeToHcd());
    }

    @Override
    public void onShutdown() {
        log.info("Assembly is shutting down.");
    }
    //#initialize

    //#track-location
    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        log.debug(() -> "onLocationTrackingEvent called: " + trackingEvent.toString());
        if (trackingEvent instanceof LocationUpdated) {
            LocationUpdated updated = (LocationUpdated) trackingEvent;
            Location location = updated.location();
            ICommandService hcd = CommandServiceFactory.jMake((AkkaLocation) (location), actorContext.getSystem());
            commandSender.tell(new SendCommand(hcd));
        } else if (trackingEvent instanceof LocationRemoved) {
            log.info("HCD no longer available");
        }
    }
    //#track-location

    private final EventKey counterEventKey = new EventKey(Prefix.apply("csw.samplehcd"), new EventName("HcdCounter"));
    private final Key<Integer> hcdCounterKey = JKeyType.IntKey().make("counter", JUnits.NoUnits);

    //#subscribe
    private void processEvent(Event event) {
        log.info("Event received: " + event.eventKey());
        if (event instanceof SystemEvent) {
            SystemEvent sysEvent = (SystemEvent) event;
            if (event.eventKey().equals(counterEventKey)) {
                int counter = sysEvent.parameter(hcdCounterKey).head();
                log.info("Counter = " + counter);
                setCounterAlarm(counter);
            } else {
                log.warn("Unexpected event received.");
            }
        } else {
            // ObserveEvent, not expected
            log.warn("Unexpected ObserveEvent received.");
        }
    }
    //#subscribe

    private IEventSubscription subscribeToHcd() {
        log.info("Starting subscription.");
        return cswCtx.eventService().defaultSubscriber().subscribeCallback(Set.of(counterEventKey), this::processEvent);
    }

    private void unsubscribeHcd() {
        log.info("Stopping subscription.");
        maybeEventSubscription.ifPresent(IEventSubscription::unsubscribe);
    }

    //#alarm
    private AlarmSeverity getCounterSeverity(int counter) {
        if (counter >= 0 && counter <= 10) {
            return JAlarmSeverity.Okay;
        } else if (counter >= 11 && counter <= 15) {
            return JAlarmSeverity.Warning;
        } else if (counter >= 16 && counter <= 20) {
            return JAlarmSeverity.Major;
        }
        return JAlarmSeverity.Critical;
    }

    private void setCounterAlarm(int counter) {
        AlarmKey counterAlarmKey = new AlarmKey(cswCtx.componentInfo().prefix(), "CounterTooHighAlarm");
        AlarmSeverity severity = getCounterSeverity(counter);
        cswCtx.alarmService().setSeverity(counterAlarmKey, severity)
                .whenComplete((d, ex) -> {
                    if (ex != null) {
                        log.error("Error setting severity for alarm " + counterAlarmKey.name() + ": " + ex.getMessage());
                    } else {
                        log.info("Severity for alarm " + counterAlarmKey.name() + " set to " + severity.toString());
                    }
                });
    }
    //#alarm

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        return null;
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        return new CommandResponse.Completed(runId);
    }

    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {
    }

    @Override
    public void onGoOffline() {
    }

    @Override
    public void onGoOnline() {
    }

    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
    }

    @Override
    public void onOperationsMode() {
    }
}
