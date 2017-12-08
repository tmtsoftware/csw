package csw.trombone.assembly;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.TopLevelActorMessage;
import csw.messages.framework.ComponentInfo;
import csw.messages.models.PubSub;
import csw.messages.params.states.CurrentState;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.JLoggerFactory;

public class TromboneAssemblyBehaviorFactory extends JComponentBehaviorFactory<DiagPublisherMessages> {

    public TromboneAssemblyBehaviorFactory() {
        super(DiagPublisherMessages.class);
    }

    @Override
    public JComponentHandlers<DiagPublisherMessages> jHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService,
            JLoggerFactory loggerFactory
    ) {
        return new TromboneAssemblyHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory, DiagPublisherMessages.class);
    }
}
