package csw.trombone.hcd;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import akka.typed.javadsl.AskPattern;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.TopLevelActorMessage;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.ControlCommand;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.TrackingEvent;
import csw.messages.models.PubSub;
import csw.messages.params.states.CurrentState;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.JLoggerFactory;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//#jcomponent-handler
public class JTromboneHcdHandlers extends JComponentHandlers<TromboneMessage> {

    // private state of this component
    private AxisResponse.AxisUpdate current;
    private AxisResponse.AxisStatistics stats;
    private ActorRef<AxisRequest> tromboneAxis;
    private AxisConfig axisConfig;
    private ActorContext<TopLevelActorMessage> ctx;

    public JTromboneHcdHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory,
            Class<TromboneMessage> klass
    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory, klass);
        this.ctx = ctx;
    }
    //#jcomponent-handler

    //#jInitialize-handler
    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        // fetch config (preferably from configuration service)
        CompletableFuture<Void> configInitialisation = getAxisConfig()
                .thenAccept(config -> {
                    axisConfig = config;
                    // create a worker actor which is used by this hcd
                    tromboneAxis = ctx.spawnAnonymous(AxisSimulator.behavior(axisConfig, ctx.getSelf().narrow()));
                });

        // initialise some state by using the worker actor created above
        CompletableFuture<Void> initialAxisState = AskPattern.ask(
                tromboneAxis,
                AxisRequest.InitialState::new,
                new Timeout(5, TimeUnit.SECONDS),
                ctx.getSystem().scheduler()
        ).thenAccept(axisUpdate -> current = axisUpdate).toCompletableFuture();

        // initialise some state by using the worker actor created above
        CompletableFuture<Void> initialAxisStats = AskPattern.ask(
                tromboneAxis,
                AxisRequest.GetStatistics::new,
                new Timeout(5, TimeUnit.SECONDS),
                ctx.getSystem().scheduler()
        ).thenAccept(axisStatistics -> stats = axisStatistics).toCompletableFuture();

        return CompletableFuture.allOf(configInitialisation, initialAxisStats, initialAxisState).thenApply(x -> BoxedUnit.UNIT);
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
    public void onDomainMsg(TromboneMessage tromboneMessage) {

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

    private CompletableFuture<AxisConfig> getAxisConfig() {
        return CompletableFuture.supplyAsync(() -> {
            Config config = ConfigFactory.load("tromboneHCDAxisConfig.conf");
            return AxisConfig.apply(config);
        });
    }

}
