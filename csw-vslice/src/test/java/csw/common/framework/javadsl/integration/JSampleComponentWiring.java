package csw.common.framework.javadsl.integration;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.javadsl.JComponentWiring;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMsg;
import csw.common.framework.models.PubSub;
import csw.param.states.CurrentState;

public class JSampleComponentWiring extends JComponentWiring<JComponentDomainMsg> {
    public JSampleComponentWiring() {
        super(JComponentDomainMsg.class);
    }

    @Override
    public JComponentHandlers<JComponentDomainMsg> make(
            ActorContext<ComponentMsg> ctx,
            ComponentInfo componentInfo,
            ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef) {
        return new JSampleComponentHandlers(ctx, componentInfo, pubSubRef, JComponentDomainMsg.class);
    }
}
