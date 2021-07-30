package csw.framework.javadsl.components;

import akka.actor.Cancellable;
import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.CommandResponseManager;
import csw.command.client.messages.TopLevelActorMessage;
import csw.common.components.framework.SampleComponentState;
import csw.event.api.javadsl.IEventService;
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.CommandResponse.SubmitResponse;
import csw.params.commands.ControlCommand;
import csw.params.core.models.Id;
import csw.params.core.models.ObsId;
import csw.params.core.states.CurrentState;
import csw.params.core.states.StateName;
import csw.params.events.IRDetectorEvent;
import csw.params.events.ObserveEvent;
import csw.params.events.OpticalDetectorEvent;
import csw.params.events.WFSDetectorEvent;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.time.core.models.UTCTime;

import java.util.Optional;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class JDetectorComponentHandlers extends JComponentHandlers {

    // Demonstrating logger accessibility in Java Component handlers
    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private CurrentStatePublisher currentStatePublisher;
    private CurrentState currentState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateName"));
    private ActorContext<TopLevelActorMessage> actorContext;
    private IEventService eventService;
    private Optional<Cancellable> diagModeCancellable = Optional.empty();

    JDetectorComponentHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
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

        //#CSW-118 : publishing observe events for IR, Optical & WFS detectors
        ObsId obsId = ObsId.apply("2020A-001-123");
        String exposureId = "some_exposure_id";
        Prefix filterHcdPrefix = new Prefix(JSubsystem.WFOS, "blue.filter.hcd");

        ObserveEvent observeStart = IRDetectorEvent.observeStart(filterHcdPrefix, obsId);
        ObserveEvent exposureStart = OpticalDetectorEvent.exposureStart(filterHcdPrefix, obsId, exposureId);
        ObserveEvent publishSuccess = WFSDetectorEvent.publishSuccess(filterHcdPrefix);
        eventService.defaultPublisher().publish(observeStart);
        eventService.defaultPublisher().publish(exposureStart);
        eventService.defaultPublisher().publish(publishSuccess);
        //#CSW-118
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        return null;
    }

    @Override
    public SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        return null;
    }

    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {
    }

    private SubmitResponse processSubmitCommand(Id runId, ControlCommand controlCommand) {
        return null;
    }


    @Override
    public void onShutdown() {
    }

    @Override
    public void onGoOffline() {
    }

    @Override
    public void onGoOnline() {
    }

    //#onDiagnostic-mode
    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
    }
    //#onDiagnostic-mode

    //#onOperations-mode
    @Override
    public void onOperationsMode() {
    }
    //#onOperations-mode
}
