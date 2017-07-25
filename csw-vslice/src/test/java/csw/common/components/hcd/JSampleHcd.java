package csw.common.components.hcd;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.hcd.messages.HcdSampleMessages;
import csw.common.framework.javadsl.JHcdActor;
import csw.common.framework.models.HcdResponseMode;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.ToComponentLifecycleMessage;
import csw.param.Parameters;

import java.util.concurrent.CompletableFuture;

public class JSampleHcd extends JHcdActor<HcdSampleMessages> {
    public JSampleHcd(ActorContext<HcdMsg> ctx, ActorRef<HcdResponseMode> supervisor, Class<HcdSampleMessages> klass) {
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
    public Void jOnDomainMsg(HcdSampleMessages hcdSampleMessages) { return null; }

    @Override
    public Void jOnInitialHcdShutdownComplete() { return null; }

    @Override
    public Void jOnLifecycle(ToComponentLifecycleMessage message) { return null; }
}
