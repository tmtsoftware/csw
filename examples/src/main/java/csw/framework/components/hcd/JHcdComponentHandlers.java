package csw.framework.components.hcd;

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

//#jcomponent-handlers-class
public class JHcdComponentHandlers extends JComponentHandlers {

    public JHcdComponentHandlers(
            akka.typed.javadsl.ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory

    ) {
        super(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory);
    }
    //#jcomponent-handlers-class


    //#jInitialize-handler
    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        /*
         * Initialization could include following steps :
         * 1. fetch config (preferably from configuration service)
         * 2. create a worker actor which is used by this hcd
         * 3. initialise some state by using the worker actor created above
         * */

        return null;
    }
    //#jInitialize-handler

    //#onShutdown-handler
    @Override
    public CompletableFuture<BoxedUnit> jOnShutdown() {
        return null;
    }
    //#onShutdown-handler

    //#onLocationTrackingEvent-handler
    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }
    //#onLocationTrackingEvent-handler

    //#validateCommand-handler
    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        return null;
    }
    //#validateCommand-handler

    //#onSubmit-handler
    @Override
    public void onSubmit(ControlCommand controlCommand) {

    }
    //#onSubmit-handler

    //#onOneway-handler
    @Override
    public void onOneway(ControlCommand controlCommand) {

    }
    //#onOneway-handler

    //#onGoOffline-handler
    @Override
    public void onGoOffline() {

    }
    //#onGoOffline-handler

    //#onGoOnline-handler
    @Override
    public void onGoOnline() {

    }
    //#onGoOnline-handler
}
