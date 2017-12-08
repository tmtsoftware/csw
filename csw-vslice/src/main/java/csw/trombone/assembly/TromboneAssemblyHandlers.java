package csw.trombone.assembly;

import akka.typed.ActorRef;
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

public class TromboneAssemblyHandlers extends JComponentHandlers<DiagPublisherMessages> {
    public TromboneAssemblyHandlers(
            akka.typed.javadsl.ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory,
            Class<DiagPublisherMessages> klass

    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory, klass);
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        return null;
    }

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
}
