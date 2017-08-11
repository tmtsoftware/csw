package csw.common.framework.scaladsl

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.common.framework.internal.Supervisor
import csw.common.framework.models.{ComponentInfo, SupervisorMsg}

object SupervisorBehaviorFactory {
  def make(componentInfo: ComponentInfo): Behavior[SupervisorMsg] = {
    val componentWiringClass = Class.forName(componentInfo.componentClassName)
    val compWring            = componentWiringClass.newInstance().asInstanceOf[ComponentWiring[_]]
    Actor.mutable[SupervisorMsg](ctx => new Supervisor(ctx, componentInfo, compWring))
  }
}
