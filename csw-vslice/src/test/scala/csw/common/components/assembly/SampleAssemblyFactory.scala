package csw.common.components.assembly

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{AssemblyComponentLifecycleMessage, AssemblyMsg, Component}
import csw.common.framework.scaladsl.{AssemblyActor, AssemblyActorFactory}

class SampleAssemblyFactory extends AssemblyActorFactory[AssemblyDomainMessages] {
  override def make(ctx: ActorContext[AssemblyMsg],
                    assemblyInfo: Component.AssemblyInfo,
                    supervisor: ActorRef[AssemblyComponentLifecycleMessage]): AssemblyActor[AssemblyDomainMessages] =
    new SampleAssembly(ctx, assemblyInfo, supervisor)
}
