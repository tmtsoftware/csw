package csw.common.components.hcd;

import akka.typed.javadsl.ActorContext;
import akka.typed.ActorRef;
import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.common.components.hcd.messages.HcdSampleMessages;
import csw.common.framework.javadsl.JHcdActor;
import csw.common.framework.models.HcdComponentLifecycleMessage;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.ToComponentLifecycleMessage;
import csw.param.Parameters;

import java.util.concurrent.CompletableFuture;

public class JSampleHcd extends JHcdActor<HcdSampleMessages> {
    private JSampleHcd(ActorContext<HcdMsg> ctx, ActorRef<HcdComponentLifecycleMessage> supervisor) {
        super(ctx, supervisor, scala.reflect.ClassTag$.MODULE$.apply(HcdSampleMessages.class));
    }

    public static Behavior<HcdMsg> behavior(ActorRef<HcdComponentLifecycleMessage> supervisor) {
        return Actor.deferred((ActorContext<HcdMsg> ctx) -> new JSampleHcd(ctx, supervisor)).narrow();
    }

    @Override
    public CompletableFuture<Void> jInitialize() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.complete(null);
        return completableFuture;
    }

    @Override
    public Void jOnRun() {
        return null;
    }

    @Override
    public Void jOnShutdown() {
        return null;
    }

    @Override
    public Void jOnShutdownComplete() {
        return null;
    }

    @Override
    public Void jOnLifecycle(ToComponentLifecycleMessage x) {
        return null;
    }

    @Override
    public Void jOnSetup(Parameters.Setup sc) {
        return null;
    }

    @Override
    public Void jOnDomainMsg(HcdSampleMessages hcdSampleMessages) {
        return null;
    }
}
