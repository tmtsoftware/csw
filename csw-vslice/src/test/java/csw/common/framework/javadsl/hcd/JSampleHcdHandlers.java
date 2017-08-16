package csw.common.framework.javadsl.hcd;


import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.ccs.Validation;
import csw.common.components.ComponentDomainMsg;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.models.CommandMsg;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMsg;
import csw.common.framework.models.PubSub;
import csw.param.states.CurrentState;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;

public class JSampleHcdHandlers extends JComponentHandlers<ComponentDomainMsg> {

    public JSampleHcdHandlers(ActorContext<ComponentMsg> ctx, ComponentInfo componentInfo, ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef, Class<ComponentDomainMsg> klass) {
        super(ctx, componentInfo, pubSubRef, klass);
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        return null;
    }

    @Override
    public void onRun() {

    }

    @Override
    public void onDomainMsg(ComponentDomainMsg hcdDomainMsg) {

    }

    @Override
    public Validation onControlCommand(CommandMsg commandMsg) {
        return null;
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onRestart() {

    }

    @Override
    public void onGoOffline() {

    }

    @Override
    public void onGoOnline() {

    }
}
