package csw.common.components.hcd;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.hcd.messages.JHcdDomainMessages;
import csw.common.framework.javadsl.hcd.JHcdActor;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.HcdResponseMode;
import csw.common.framework.models.LifecycleState;
import csw.param.Parameters;

import java.util.concurrent.CompletableFuture;

public class JSampleHcd extends JHcdActor<JHcdDomainMessages> {
    JSampleHcd(ActorContext<HcdMsg> ctx, ActorRef<HcdResponseMode> supervisor, Class<JHcdDomainMessages> klass) {
        super(ctx, supervisor, klass);
    }

    @Override
    public CompletableFuture jInitialize() {
        CompletableFuture completableFuture = new CompletableFuture();
        completableFuture.complete(null);
        return completableFuture;
    }

    @Override
    public void jOnInitialRun() {}

    @Override
    public void jOnSetup(Parameters.Setup sc) {}

    @Override
    public void jOnDomainMsg(JHcdDomainMessages hcdSampleMessages) {}

    @Override
    public void jOnShutdown() {}

    @Override
    public void jOnRestart() {}

    @Override
    public void jOnRun() {}

    @Override
    public void jOnGoOffline() {}

    @Override
    public void jOnGoOnline() {}

    @Override
    public void jOnLifecycleFailureInfo(LifecycleState state, String reason) {}
}
