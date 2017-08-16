package csw.common.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.Validation
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{CommandMsg, ComponentInfo, ComponentMsg}
import csw.param.states.CurrentState

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ComponentHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg],
                                                             componentInfo: ComponentInfo,
                                                             pubSubRef: ActorRef[PublisherMsg[CurrentState]]) {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onDomainMsg(msg: Msg): Unit
  def onControlCommand(commandMsg: CommandMsg): Validation
  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}
