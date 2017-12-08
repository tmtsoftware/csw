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

public class TromboneHcdHandlers extends JComponentHandlers<TromboneMessage> {

    private AxisResponse.AxisUpdate current;
    private AxisResponse.AxisStatistics stats;
    private ActorRef<AxisRequest> tromboneAxis;
    private AxisConfig axisConfig;
    private ActorContext<TopLevelActorMessage> context;

    public TromboneHcdHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory,
            Class<TromboneMessage> klass
    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory, klass);
        context = ctx;
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        CompletableFuture<Void> future = getAxisConfig()
                .thenAccept(config -> {
                    axisConfig = config;
                    tromboneAxis = context.spawnAnonymous(AxisSimulator.behavior(axisConfig, context.getSelf().narrow()));
                });

        CompletableFuture<Void> voidCompletableFuture1 = AskPattern.ask(
                tromboneAxis,
                AxisRequest.InitialState::new,
                new Timeout(5, TimeUnit.SECONDS),
                context.getSystem().scheduler()
        ).thenAccept(axisUpdate -> current = axisUpdate).toCompletableFuture();

        CompletableFuture<Void> voidCompletableFuture = AskPattern.ask(
                tromboneAxis,
                AxisRequest.GetStatistics::new,
                new Timeout(5, TimeUnit.SECONDS),
                context.getSystem().scheduler()
        ).thenAccept(axisStatistics -> stats = axisStatistics).toCompletableFuture();

        return CompletableFuture.allOf(future, voidCompletableFuture, voidCompletableFuture1).thenApply(x -> BoxedUnit.UNIT);

    }

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
