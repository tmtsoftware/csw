package example.framework.components.hcd;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.AskPattern;
import csw.command.client.CommandResponseManager;
import csw.command.client.messages.TopLevelActorMessage;
import csw.command.client.models.framework.ComponentInfo;
import csw.config.api.ConfigData;
import csw.config.api.javadsl.IConfigClientService;
import csw.event.api.javadsl.IEventService;
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.LocationRemoved;
import csw.location.api.models.LocationUpdated;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.*;
import csw.params.core.models.Id;
import csw.time.core.models.UTCTime;
import example.framework.components.ConfigNotAvailableException;
import example.framework.components.assembly.WorkerActor;
import example.framework.components.assembly.WorkerActorMsg;
import example.framework.components.assembly.WorkerActorMsgs;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.*;

//#jcomponent-handlers-class
public class JHcdComponentHandlers extends JComponentHandlers {

    private final ActorContext<TopLevelActorMessage> ctx;
    private final ComponentInfo componentInfo;
    private final CommandResponseManager commandResponseManager;
    private final CurrentStatePublisher currentStatePublisher;
    private final ILocationService locationService;
    private final IEventService eventService;
    private ILogger log;
    private IConfigClientService configClient;
    private ConfigData hcdConfig;
    private ActorRef<WorkerActorMsg> worker;
    private int current;
    private int stats;
    private int timeout = 10;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    public JHcdComponentHandlers(
            akka.actor.typed.javadsl.ActorContext<TopLevelActorMessage> ctx,
            JCswContext cswCtx
    ) {
        super(ctx, cswCtx);
        this.ctx = ctx;
        this.componentInfo = cswCtx.componentInfo();
        this.commandResponseManager = cswCtx.commandResponseManager();
        this.currentStatePublisher = cswCtx.currentStatePublisher();
        this.locationService = cswCtx.locationService();
        this.eventService = cswCtx.eventService();
        log = cswCtx.loggerFactory().getLogger(this.getClass());
    }
    //#jcomponent-handlers-class


    //#jInitialize-handler
    @Override
    public void initialize() {

        // fetch config (preferably from configuration service)
        try {
            hcdConfig = getConfig().get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new RuntimeException("Can not get config ", ex);
        }

        // create a worker actor which is used by this hcd
        worker = ctx.spawnAnonymous(WorkerActor.behavior(hcdConfig));

        // initialise some state by using the worker actor created above
        try {
            CompletionStage<Integer> askCurrent = AskPattern.ask(worker, WorkerActorMsgs.JInitialState::new, Duration.ofSeconds(5), ctx.getSystem().scheduler());
            askCurrent.thenAccept(c -> current = c).toCompletableFuture().get(timeout, timeUnit);

            CompletionStage<Integer> askStats = AskPattern.ask(worker, WorkerActorMsgs.JInitialState::new, Duration.ofSeconds(5), ctx.getSystem().scheduler());
            askStats.thenAccept(s -> stats = s).toCompletableFuture().get(timeout, timeUnit);

        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new RuntimeException(ex);
        }
    }

    //#jInitialize-handler
    //#validateCommand-handler
    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        if (controlCommand instanceof Setup) {
            // validation for setup goes here
            return new CommandResponse.Accepted(runId);
        } else if (controlCommand instanceof Observe) {
            // validation for observe goes here
            return new CommandResponse.Accepted(runId);
        } else {
            return new CommandResponse.Invalid(runId, new CommandIssue.HCDBusyIssue("HCD is currently processing other command"));
        }
    }
    //#validateCommand-handler

    //#onSubmit-handler
    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        if (controlCommand instanceof Setup)
            return submitSetup(runId, (Setup) controlCommand); // includes logic to handle Submit with Setup config command
        else if (controlCommand instanceof Observe)
            return submitObserve(runId, (Observe) controlCommand); // includes logic to handle Submit with Observe config command"
        else
            return new CommandResponse.Error(runId, "Unknown command: " + controlCommand.commandName().name());
    }
    //#onSubmit-handler

    //#onOneway-handler
    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {
        if (controlCommand instanceof Setup)
            onewaySetup(runId, (Setup) controlCommand); // includes logic to handle Oneway with Setup config command
        else if (controlCommand instanceof Observe)
            onewayObserve(runId, (Observe) controlCommand); // includes logic to handle Oneway with Observe config command
    }
    //#onOneway-handler

    //#onGoOffline-handler
    @Override
    public void onGoOffline() {
        // do something when going offline
    }
    //#onGoOffline-handler

    //#onGoOnline-handler
    @Override
    public void onGoOnline() {
        // do something when going online
    }
    //#onGoOnline-handler

    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
    }

    @Override
    public void onOperationsMode() {
    }

    //#onShutdown-handler
    @Override
    public void onShutdown() {
        // clean up resources
    }
    //#onShutdown-handler

    //#onLocationTrackingEvent-handler
    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        if (trackingEvent instanceof LocationUpdated) {
            // do something for the tracked location when it is updated
        } else if (trackingEvent instanceof LocationRemoved) {
            // do something for the tracked location when it is no longer available
        }
    }
    //#onLocationTrackingEvent-handler

    private CommandResponse.SubmitResponse processSetup(Id runId, Setup sc) {
        switch (sc.commandName().name()) {
            case "axisMove":
            case "axisDatum":
            case "axisHome":
            case "axisCancel":
            default:
                log.error("Invalid command [" + sc + "] received.");
        }
        return new CommandResponse.Completed(runId);
    }

    private CommandResponse.SubmitResponse processObserve(Id runId, Observe oc) {
        switch (oc.commandName().name()) {
            case "point":
            case "acquire":
            default:
                log.error("Invalid command [" + oc + "] received.");
        }
        return new CommandResponse.Completed(runId);
    }

    /**
     * in case of submit command, component writer is required to update commandResponseManager with the result
     */
    private CommandResponse.SubmitResponse submitSetup(Id runId, Setup setup) {
        return processSetup(runId, setup);
    }

    private CommandResponse.SubmitResponse submitObserve(Id runId, Observe observe) {
        return processObserve(runId, observe);
    }

    private void onewaySetup(Id runId, Setup setup) {
        processSetup(runId, setup);
    }

    private void onewayObserve(Id runId, Observe observe) {
        processObserve(runId, observe);
    }

    private CompletableFuture<ConfigData> getConfig() {
        // required configuration could not be found in the configuration service. Component can choose to stop until the configuration is made available in the
        // configuration service and started again
        return configClient.getActive(Paths.get("tromboneAssemblyContext.conf")).thenApply((maybeConfigData) -> maybeConfigData.<ConfigNotAvailableException>orElseThrow(ConfigNotAvailableException::new));
    }
}
