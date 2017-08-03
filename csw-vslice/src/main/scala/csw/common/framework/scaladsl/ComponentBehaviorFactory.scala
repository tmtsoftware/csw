package csw.common.framework.scaladsl

import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.FromComponentLifecycleMessage

abstract class ComponentBehaviorFactory[CompInfo <: ComponentInfo] {
  def behavior(compInfo: CompInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing]
}
