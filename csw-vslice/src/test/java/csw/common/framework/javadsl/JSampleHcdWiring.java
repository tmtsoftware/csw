package csw.common.framework.javadsl;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.ComponentDomainMsg;
import csw.common.framework.javadsl.hcd.JSampleHcdHandlers;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMsg;
import csw.common.framework.models.PubSub;
import csw.param.states.CurrentState;

public class JSampleHcdWiring extends JComponentWiring<ComponentDomainMsg> {
    public JSampleHcdWiring() {
        super(ComponentDomainMsg.class);
    }

    @Override
    public JComponentHandlers<ComponentDomainMsg> make(
            ActorContext<ComponentMsg> ctx,
            ComponentInfo componentInfo,
            ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef) {
        return new JSampleHcdHandlers(ctx, componentInfo, pubSubRef, ComponentDomainMsg.class);
    }
}
