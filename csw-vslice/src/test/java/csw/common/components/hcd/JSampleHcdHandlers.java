package csw.common.components.hcd;

import akka.typed.javadsl.ActorContext;
import csw.common.components.hcd.messages.JHcdDomainMessages;
import csw.common.framework.javadsl.hcd.JHcdHandlers;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.LifecycleState;
import csw.param.Parameters;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;

public class JSampleHcdHandlers extends JHcdHandlers<JHcdDomainMessages> {

    JSampleHcdHandlers(ActorContext<HcdMsg> ctx, Class<JHcdDomainMessages> klass) {
        super(ctx, klass);
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        return null;
    }

    @Override
    public void onRun() {

    }

    @Override
    public void onSetup(Parameters.Setup sc) {

    }

    @Override
    public void onDomainMsg(JHcdDomainMessages jHcdDomainMessages) {

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

    @Override
    public void onLifecycleFailureInfo(LifecycleState state, String reason) {

    }
}
