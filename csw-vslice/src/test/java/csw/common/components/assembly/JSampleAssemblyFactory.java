package csw.common.components.assembly;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.assembly.messages.JAssemblyDomainMessages;
import csw.common.framework.javadsl.JAssemblyActor;
import csw.common.framework.javadsl.JAssemblyActorFactory;
import csw.common.framework.models.AssemblyComponentLifecycleMessage;
import csw.common.framework.models.AssemblyMsg;
import csw.common.framework.models.Component;

public class JSampleAssemblyFactory extends JAssemblyActorFactory<JAssemblyDomainMessages> {

    private final Class<JAssemblyDomainMessages> klass;

    public JSampleAssemblyFactory(Class<JAssemblyDomainMessages> klass) {
        super(klass);
        this.klass = klass;
    }

    @Override
    public JAssemblyActor<JAssemblyDomainMessages> make(ActorContext<AssemblyMsg> ctx, Component.AssemblyInfo assemblyInfo, ActorRef<AssemblyComponentLifecycleMessage> supervisor) {
        return new JSampleAssembly(ctx, assemblyInfo, supervisor);
    }
}
