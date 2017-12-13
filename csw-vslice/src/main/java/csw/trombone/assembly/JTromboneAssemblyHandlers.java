package csw.trombone.assembly;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import com.typesafe.config.ConfigFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.TopLevelActorMessage;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.ControlCommand;
import csw.messages.ccs.commands.JWrappedComponent;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.AkkaLocation;
import csw.messages.location.Connection;
import csw.messages.location.TrackingEvent;
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

//#jcomponent-handler
public class JTromboneAssemblyHandlers extends JComponentHandlers<DiagPublisherMessages> {

    // private state of this component
    private AssemblyContext ac;
    private ComponentInfo componentInfo;
    private ActorContext<TopLevelActorMessage> ctx;
    private ILocationService locationService;
    private Map<Connection, Optional<JWrappedComponent>> runningHcds;
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
    //#jcomponent-handler
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

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public void onDomainMsg(DiagPublisherMessages diagPublisherMessages) {

    }

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

    private CompletableFuture<TromboneCalculationConfig> getAssemblyCalculationConfig() {
        return CompletableFuture.supplyAsync(() -> ConfigFactory.load("tromboneAssemblyContext.conf"))
                .thenApply(TromboneCalculationConfig::apply);
    }

    private CompletableFuture<TromboneControlConfig> getAssemblyControlConfig() {
        return CompletableFuture.supplyAsync(() -> ConfigFactory.load("tromboneAssemblyContext.conf"))
                .thenApply(TromboneControlConfig::apply);
    }

}
