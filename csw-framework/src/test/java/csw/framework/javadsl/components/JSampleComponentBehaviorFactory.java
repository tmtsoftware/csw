package csw.framework.javadsl.components;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.messages.CommandResponseManagerMessage;
import csw.messages.framework.ComponentInfo;
import csw.messages.ComponentMessage;
import csw.messages.PubSub;
import csw.messages.params.states.CurrentState;
import csw.services.location.javadsl.ILocationService;

public class JSampleComponentBehaviorFactory extends JComponentBehaviorFactory<JComponentDomainMessage> {
    public JSampleComponentBehaviorFactory() {
        super(JComponentDomainMessage.class);
    }

    @Override
    public JComponentHandlers<JComponentDomainMessage> jHandlers(
            ActorContext<ComponentMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<CommandResponseManagerMessage> commandResponseManager,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef,
            ILocationService locationService) {
        return new JSampleComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, JComponentDomainMessage.class);
    }
}
