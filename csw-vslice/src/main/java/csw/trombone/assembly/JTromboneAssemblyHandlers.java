package csw.trombone.assembly;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import com.typesafe.config.ConfigFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.TopLevelActorMessage;
import csw.messages.ccs.CommandIssue;
import csw.messages.ccs.commands.*;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.*;
import csw.messages.models.PubSub;
import csw.messages.params.states.CurrentState;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JComponentType;
import csw.services.logging.javadsl.JLoggerFactory;
import csw.trombone.assembly.actors.DiagPublisher;
import csw.trombone.assembly.actors.TrombonePublisher;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//#jcomponent-handlers-class
public class JTromboneAssemblyHandlers extends JComponentHandlers<DiagPublisherMessages> {

    // private state of this component
    private AssemblyContext ac;
    private ComponentInfo componentInfo;
    private ActorContext<TopLevelActorMessage> ctx;
    private ILocationService locationService;
    private Map<Connection, Optional<JComponentRef>> runningHcds;
    private ActorRef<DiagPublisherMessages> diagPublisher;

    public JTromboneAssemblyHandlers(
            akka.typed.javadsl.ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory,
            Class<DiagPublisherMessages> klass

    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory, klass);
        this.componentInfo = componentInfo;
        this.ctx = ctx;
        this.locationService = locationService;
        runningHcds = new HashMap<>();
    }

    //#jcomponent-handlers-class
    //#jInitialize-handler
    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        // fetch config (preferably from configuration service)
        CompletableFuture<AssemblyContext> assemblyContextCompletableFuture = getAssemblyCalculationConfig()
                .thenCombine(getAssemblyControlConfig(),
                        (calculationConf, controlConf) -> ac = new AssemblyContext(componentInfo, calculationConf, controlConf));

        // create a worker actor which is used by this assembly
        CompletableFuture<ActorRef<TrombonePublisherMsg>> eventualEventPublisher =
                assemblyContextCompletableFuture.thenApply(ac -> ctx.spawnAnonymous(TrombonePublisher.make(ac)));

        // find a Hcd connection from the connections provided in componentInfo
        Optional<Connection> mayBeConnection = componentInfo.getConnections().stream()
                .filter(connection -> connection.componentId().componentType() == JComponentType.HCD)
                .findFirst();

        // If an Hcd is found as a connection, resolve its location from location service and create other
        // required worker actors required by this assembly
        Optional<CompletableFuture<Void>> voidCompletableFuture = mayBeConnection.map(connection -> {
            CompletableFuture<Optional<AkkaLocation>> eventualHcdLocation = locationService.resolve(connection.<AkkaLocation>of(), FiniteDuration.apply(5, TimeUnit.SECONDS));

            return eventualEventPublisher.thenAcceptBoth(eventualHcdLocation, (eventPublisher, hcdLocation) -> {
                hcdLocation.map(hcd -> {
                    runningHcds.put(connection, Optional.of(hcd.jComponent()));
                    diagPublisher = ctx.spawnAnonymous(DiagPublisher.jMake(ac, Optional.of(hcd.component()), Optional.of(eventPublisher)));
                    return Optional.empty();
                });
            });
        });

        return voidCompletableFuture.get().thenApply(x -> BoxedUnit.UNIT);
    }
    //#jInitialize-handler

    @Override
    public CompletableFuture<BoxedUnit> jOnShutdown() {
        return null;
    }

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

    @Override
    public void onDomainMsg(DiagPublisherMessages diagPublisherMessages) {

    }

    // #validateCommand-handler
    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup) {
            return new CommandResponse.Completed(controlCommand.runId());
        } else if (controlCommand instanceof Observe) {
            return new CommandResponse.Completed(controlCommand.runId());
        } else {
            return new CommandResponse.Invalid(controlCommand.runId(), new CommandIssue.UnsupportedCommandIssue("command" + controlCommand + "is not supported by this component."));
        }
    }
    // #validateCommand-handler

    @Override
    public void onSubmit(ControlCommand controlCommand) {

    }

    @Override
    public void onOneway(ControlCommand controlCommand) {

    }

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

    private CompletableFuture<TromboneCalculationConfig> getAssemblyCalculationConfig() {
        return CompletableFuture.supplyAsync(() -> ConfigFactory.load("tromboneAssemblyContext.conf"))
                .thenApply(TromboneCalculationConfig::apply);
    }

    private CompletableFuture<TromboneControlConfig> getAssemblyControlConfig() {
        return CompletableFuture.supplyAsync(() -> ConfigFactory.load("tromboneAssemblyContext.conf"))
                .thenApply(TromboneControlConfig::apply);
    }

}
