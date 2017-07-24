package csw.common.components.assembly;

import akka.typed.ActorRef;
import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.common.ccs.CommandStatus;
import csw.common.ccs.Validation;
import csw.common.components.assembly.messages.JAssemblyDomainMessages;
import csw.common.framework.javadsl.JAssemblyActor;
import csw.common.framework.models.AssemblyComponentLifecycleMessage;
import csw.common.framework.models.AssemblyMsg;
import csw.common.framework.models.Component.AssemblyInfo;
import csw.common.framework.models.ToComponentLifecycleMessage;
import csw.param.Parameters;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JSampleAssembly extends JAssemblyActor<JAssemblyDomainMessages> {

    public JSampleAssembly(ActorContext<AssemblyMsg> ctx, AssemblyInfo assemblyInfo, ActorRef<AssemblyComponentLifecycleMessage> supervisor) {
        super(ctx, assemblyInfo, supervisor, scala.reflect.ClassTag$.MODULE$.apply(JAssemblyDomainMessages.class));
    }


    public static Behavior<AssemblyMsg> behavior(AssemblyInfo assemblyInfo, ActorRef<AssemblyComponentLifecycleMessage> supervisor) {
        return Actor.deferred((ActorContext<AssemblyMsg> ctx) -> {
            JSampleAssembly jSampleAssembly = new JSampleAssembly(ctx, assemblyInfo, supervisor);
            return jSampleAssembly;
        });
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
    public Validation.Validation jSetup(Parameters.Setup s, Optional<ActorRef<CommandStatus.CommandResponse>> commandOriginator) {
        return null;
    }

    @Override
    public Validation.Validation jObserve(Parameters.Observe o, Optional<ActorRef<CommandStatus.CommandResponse>> replyTo) {
        return null;
    }

    @Override
    public Void jOnDomainMsg(JAssemblyDomainMessages jAssemblyDomainMessages) {
        return null;
    }

    @Override
    public Void jOnLifecycle(ToComponentLifecycleMessage message) {
        return null;
    }
}
