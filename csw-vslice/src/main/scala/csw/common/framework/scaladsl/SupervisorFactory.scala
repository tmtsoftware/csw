package csw.common.framework.scaladsl

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.common.framework.models.{ComponentInfo, SupervisorExternalMessage}

class SupervisorFactory {
  def make(componentInfo: ComponentInfo): ActorRef[SupervisorExternalMessage] = {
    val system = ActorSystem(s"${componentInfo.name}-system")
    system.spawn(SupervisorBehaviorFactory.behavior(componentInfo), componentInfo.name)
  }
}
