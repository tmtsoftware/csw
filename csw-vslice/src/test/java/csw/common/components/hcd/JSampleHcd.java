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
    public JSampleHcd(ActorContext<HcdMsg> ctx, ActorRef<HcdResponseMode> supervisor, Class<JHcdDomainMessages> klass) {
        super(ctx, supervisor, klass);
    }

    @Override
    public CompletableFuture<Void> jInitialize() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.complete(null);
        return completableFuture;
    }

    @Override
    public Void jOnInitialRun() {
        return null;
    }

    @Override
    public Void jOnRunningHcdShutdownComplete() { return null; }

    @Override
    public Void jOnSetup(Parameters.Setup sc) {
        return null;
    }

    @Override
    public Void jOnDomainMsg(JHcdDomainMessages hcdSampleMessages) { return null; }

    @Override
    public Void jOnInitialHcdShutdownComplete() { return null; }

    @Override
    public Void jOnShutdown() { return null; }

    @Override
    public Void jOnRestart() { return null; }

    @Override
    public Void jOnRun() { return null; }

    @Override
    public Void jOnRunOffline() { return null; }

    @Override
    public Void jOnLifecycleFailureInfo(LifecycleState state, String reason) { return null; }

    @Override
    public Void jOnShutdownComplete() { return null; }
}
