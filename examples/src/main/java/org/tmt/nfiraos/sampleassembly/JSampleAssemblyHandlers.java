package org.tmt.nfiraos.sampleassembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.util.Timeout;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.scaladsl.CurrentStatePublisher;
import csw.messages.commands.CommandName;
import csw.messages.commands.CommandResponse;
import csw.messages.commands.ControlCommand;
import csw.messages.commands.Setup;
import csw.messages.framework.ComponentInfo;
import csw.messages.javadsl.JUnits;
import csw.messages.location.*;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.ObsId;
import csw.messages.params.models.Prefix;
import csw.messages.scaladsl.TopLevelActorMessage;
import csw.services.command.javadsl.JCommandService;
import csw.services.command.scaladsl.CommandResponseManager;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

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
    private ComponentInfo componentInfo;

    private final ActorRef<WorkerCommand> commandSender;

    JSampleAssemblyHandlers(
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
        this.locationService = locationService;
        this.componentInfo = componentInfo;
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
        Key<Long> sleepTimeKey = JKeyTypes.LongKey().make("SleepTime");
        Parameter<Long> sleepTimeParam = sleepTimeKey.set(5000L).withUnits(JUnits.millisecond);

        Setup setupCommand = new Setup(new Prefix(componentInfo.prefix()), new CommandName("sleep"), Optional.of(new ObsId("2018A-001"))).add(sleepTimeParam);

        Timeout submitTimeout = new Timeout(1, TimeUnit.SECONDS);
        Timeout commandResponseTimeout = new Timeout(10, TimeUnit.SECONDS);

        // Submit command, and handle validation response. Final response is returned as a Future
        CompletableFuture<CommandResponse> submitCommandResponseF = hcd.submit(setupCommand, submitTimeout)
                .thenCompose(commandResponse -> {
                    if (commandResponse instanceof CommandResponse.Accepted) {
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
    @Override
    public CompletableFuture<Void> jInitialize() {
        return CompletableFuture.runAsync(() -> log.info("In Assembly initialize"));
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

    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        return null;
    }

    @Override
    public void onSubmit(ControlCommand controlCommand) {
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
