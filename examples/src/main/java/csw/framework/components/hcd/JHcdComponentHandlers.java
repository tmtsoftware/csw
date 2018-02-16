package csw.framework.components.hcd;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.scaladsl.CurrentStatePublisher;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.TopLevelActorMessage;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.ControlCommand;
import csw.messages.ccs.commands.Observe;
import csw.messages.ccs.commands.Setup;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.LocationRemoved;
import csw.messages.location.LocationUpdated;
import csw.messages.location.TrackingEvent;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

import java.util.concurrent.CompletableFuture;

//#jcomponent-handlers-class
public class JHcdComponentHandlers extends JComponentHandlers {

    private final ActorContext<TopLevelActorMessage> ctx;
    private final ComponentInfo componentInfo;
    private final ActorRef<CommandResponseManagerMessage> commandResponseManager;
    private final CurrentStatePublisher currentStatePublisher;
    private final ILocationService locationService;
    private ILogger log;

    public JHcdComponentHandlers(
            akka.typed.javadsl.ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            CurrentStatePublisher currentStatePublisher,
            ILocationService locationService,
            JLoggerFactory loggerFactory

    ) {
        super(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory);
        this.ctx = ctx;
        this.componentInfo = componentInfo;
        this.commandResponseManager = commandResponseManager;
        this.currentStatePublisher = currentStatePublisher;
        this.locationService = locationService;
        log = loggerFactory.getLogger(this.getClass());
    }
    //#jcomponent-handlers-class


    //#jInitialize-handler
    @Override
    public CompletableFuture<Void> jInitialize() {
        return CompletableFuture.runAsync(() -> {
            /*
             * Initialization could include following steps :
             * 1. fetch config (preferably from configuration service)
             * 2. create a worker actor which is used by this hcd
             * 3. initialise some state by using the worker actor created above
             * */
        });
    }
    //#jInitialize-handler

    //#validateCommand-handler
    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup) {
            // validation for setup goes here
            return new CommandResponse.Accepted(controlCommand.runId());
        } else if (controlCommand instanceof Observe) {
            // validation for observe goes here
            return new CommandResponse.Accepted(controlCommand.runId());
        } else {
            return new CommandResponse.CommandNotAvailable(controlCommand.runId());
        }
    }
    //#validateCommand-handler

    //#onSubmit-handler
    @Override
    public void onSubmit(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup)
            submitSetup((Setup) controlCommand); // includes logic to handle Submit with Setup config command
        else if (controlCommand instanceof Observe)
            submitObserve((Observe) controlCommand); // includes logic to handle Submit with Observe config command
    }
    //#onSubmit-handler

    //#onOneway-handler
    @Override
    public void onOneway(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup)
            onewaySetup((Setup) controlCommand); // includes logic to handle Oneway with Setup config command
        else if (controlCommand instanceof Observe)
            onewayObserve((Observe) controlCommand); // includes logic to handle Oneway with Observe config command
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

    //#onShutdown-handler
    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> {
            // clean up resources
        });
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

    private void processSetup(Setup sc) {
        switch (sc.commandName().name()) {
            case "axisMove":
            case "axisDatum":
            case "axisHome":
            case "axisCancel":
            default:
                log.error("Invalid command [" + sc + "] received.");
        }
    }

    private void processObserve(Observe oc) {
        switch (oc.commandName().name()) {
            case "point":
            case "acquire":
            default:
                log.error("Invalid command [" + oc + "] received.");
        }
    }

    /**
     * in case of submit command, component writer is required to update commandResponseManager with the result
     */
    private void submitSetup(Setup setup) {
        processSetup(setup);
    }

    private void submitObserve(Observe observe) {
        processObserve(observe);
    }

    private void onewaySetup(Setup setup) {
        processSetup(setup);
    }

    private void onewayObserve(Observe observe) {
        processObserve(observe);
    }
}
