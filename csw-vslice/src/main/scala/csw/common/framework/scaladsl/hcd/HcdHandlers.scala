package csw.common.framework.scaladsl.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models._
import csw.common.framework.scaladsl.{LifecycleHandlers, PubSubActor}
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

abstract class HcdHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo)
    extends LifecycleHandlers {
  val domainAdapter: ActorRef[Msg]              = ctx.spawnAdapter(DomainHcdMsg.apply)
  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behavior[CurrentState])

  implicit val ec: ExecutionContext = ctx.executionContext

  def onSetup(sc: Setup): Unit

  def stopChildren(): Unit = {
    ctx.stop(domainAdapter)
    ctx.stop(pubSubRef)
  }
}
