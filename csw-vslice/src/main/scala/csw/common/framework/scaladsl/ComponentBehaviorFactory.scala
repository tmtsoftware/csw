package csw.common.framework.scaladsl

import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.FromComponentLifecycleMessage
import csw.common.framework.models.PubSub.PublisherMsg
import csw.param.StateVariable.CurrentState

abstract class ComponentBehaviorFactory[CompInfo <: ComponentInfo] {
  def behavior(compInfo: CompInfo,
               supervisor: ActorRef[FromComponentLifecycleMessage],
               pubSubRef: ActorRef[PublisherMsg[CurrentState]]): Behavior[Nothing]
}
