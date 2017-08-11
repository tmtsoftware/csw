package csw.common.framework.javadsl.hcd;


import akka.typed.javadsl.ActorContext;
import csw.common.ccs.Validation;
import csw.common.components.hcd.HcdDomainMsg;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.models.CommandMsg;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMsg;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;

public class JSampleHcdHandlers extends JComponentHandlers<HcdDomainMsg> {

    public JSampleHcdHandlers(ActorContext<ComponentMsg> ctx, ComponentInfo componentInfo, Class<HcdDomainMsg> klass) {
        super(ctx, componentInfo, klass);
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        return null;
    }

    @Override
    public void onRun() {

    }

    @Override
    public void onDomainMsg(HcdDomainMsg hcdDomainMsg) {

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
