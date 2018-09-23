package org.tmt.nfiraos.sampleassembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.util.Timeout;
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.command.messages.TopLevelActorMessage;
import csw.params.commands.CommandName;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.commands.Setup;
import csw.params.events.Event;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.command.models.framework.ComponentInfo;
import csw.params.javadsl.JUnits;
import csw.location.api.models.*;
import csw.params.javadsl.JKeyType;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.ObsId;
import csw.params.core.models.Prefix;
import csw.command.CommandResponseManager;
import csw.command.javadsl.JCommandService;
import csw.event.api.javadsl.IEventService;
import csw.event.api.javadsl.IEventSubscription;
import csw.location.api.javadsl.ILocationService;
import csw.logging.javadsl.ILogger;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
public class JSampleAssemblyHandlers extends JComponentHandlers {

    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private CurrentStatePublisher currentStatePublisher;
    private ActorContext<TopLevelActorMessage> actorContext;
    private ILocationService locationService;
    private IEventService eventService;
    private ComponentInfo componentInfo;

    private final ActorRef<WorkerCommand> commandSender;

    JSampleAssemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.currentStatePublisher = cswCtx.currentStatePublisher();
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.commandResponseManager = cswCtx.commandResponseManager();
        this.actorContext = ctx;
        this.locationService = cswCtx.locationService();
        this.eventService = cswCtx.eventService();
        this.componentInfo = cswCtx.componentInfo();
        this.commandSender = createWorkerActor();
    }

    //#worker-actor
    private interface WorkerCommand {
    }

    private static final class SendCommand implements WorkerCommand {
        private final JCommandService hcd;

        private SendCommand(JCommandService hcd) {
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

    private void handle(JCommandService hcd) {

        // Construct Setup command
        Key<Long> sleepTimeKey = JKeyType.LongKey().make("SleepTime");
        Parameter<Long> sleepTimeParam = sleepTimeKey.set(5000L).withUnits(JUnits.millisecond);

        Setup setupCommand = new Setup(componentInfo.prefix(), new CommandName("sleep"), Optional.of(new ObsId("2018A-001"))).add(sleepTimeParam);

        Timeout submitTimeout = new Timeout(1, TimeUnit.SECONDS);
        Timeout commandResponseTimeout = new Timeout(10, TimeUnit.SECONDS);

        // Submit command, and handle validation response. Final response is returned as a Future
        CompletableFuture<CommandResponse.SubmitResponse> submitCommandResponseF = hcd.submit(setupCommand, submitTimeout)
                .thenCompose(commandResponse -> {
                    if (commandResponse instanceof CommandResponse.Started) {
                        return hcd.subscribe(commandResponse.runId(), commandResponseTimeout);
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
    public CompletableFuture<Void> jInitialize() {
        return CompletableFuture.runAsync(() -> {
            log.info("In Assembly initialize");
            maybeEventSubscription = Optional.of(subscribeToHcd());
        });
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> log.info("Assembly is shutting down."));
    }
    //#initialize

    //#track-location
    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        log.debug(() -> "onLocationTrackingEvent called: " + trackingEvent.toString());
        if (trackingEvent instanceof LocationUpdated) {
            LocationUpdated updated = (LocationUpdated) trackingEvent;
            Location location = updated.location();
            JCommandService hcd = new JCommandService((AkkaLocation) (location), actorContext.getSystem());
            commandSender.tell(new SendCommand(hcd));
        } else if (trackingEvent instanceof LocationRemoved) {
            log.info("HCD no longer available");
        }
    }
    //#track-location

    //#subscribe
    private EventKey counterEventKey = new EventKey(new Prefix("nfiraos.samplehcd"), new EventName("HcdCounter"));
    private Key<Integer> hcdCounterKey = JKeyType.IntKey().make("counter");

    private void processEvent(Event event) {
        log.info("Event received: "+ event.eventKey());
        if (event instanceof SystemEvent) {
            SystemEvent sysEvent = (SystemEvent)event;
            if (event.eventKey().equals(counterEventKey)) {
                log.info("Counter = " + sysEvent.parameter(hcdCounterKey).head());
            } else {
                log.warn("Unexpected event received.");
            }
        } else {
            // ObserveEvent, not expected
            log.warn("Unexpected ObserveEvent received.");
        }
    }

    private IEventSubscription subscribeToHcd() {
        log.info("Starting subscription.");
        return eventService.defaultSubscriber().subscribeCallback(Collections.singleton(counterEventKey), this::processEvent);
    }

    private void unsubscribeHcd() {
        log.info("Stopping subscription.");
        maybeEventSubscription.ifPresent(IEventSubscription::unsubscribe);
    }
    //#subscribe

    @Override
    public CommandResponse.ValidationResponse validateCommand(ControlCommand controlCommand) {
        return null;
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {
        return new CommandResponse.Completed(controlCommand.runId());
    }

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
