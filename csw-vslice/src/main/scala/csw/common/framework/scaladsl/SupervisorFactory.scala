package csw.common.framework.scaladsl

import akka.actor.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.common.framework.models.{ComponentInfo, SupervisorInfo}

class SupervisorFactory {
  def make(componentInfo: ComponentInfo): SupervisorInfo = {
    val system             = ActorSystem(s"${componentInfo.name}-system")
    val supervisorBehavior = SupervisorBehaviorFactory.behavior(componentInfo)
    val supervisorRef      = system.spawn(supervisorBehavior, componentInfo.name)
    SupervisorInfo(system, supervisorRef, componentInfo)
  }
}
