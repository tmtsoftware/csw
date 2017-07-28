package csw.common.framework.scaladsl.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningHcdMsg._
import csw.common.framework.models._
import csw.common.framework.scaladsl.PubSubActor
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class HcdHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo) {
  val domainAdapter: ActorRef[Msg]              = ctx.spawnAdapter(DomainHcdMsg.apply)
  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behaviour[CurrentState])

  implicit val ec: ExecutionContext = ctx.executionContext

  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onSetup(sc: Setup): Unit
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
  def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit

  def stopChildren(): Unit = {
    ctx.stop(domainAdapter)
    ctx.stop(pubSubRef)
  }
}
