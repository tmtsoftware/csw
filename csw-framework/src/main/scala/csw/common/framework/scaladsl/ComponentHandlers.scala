package csw.common.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.Validation
import csw.common.framework.models.PubSub.PublisherMessage
import csw.common.framework.models.RunningMessage.DomainMessage
import csw.common.framework.models.{CommandMessage, ComponentInfo, ComponentMessage}
import csw.param.states.CurrentState

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ComponentHandlers[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]]
) extends FrameworkLogger.Simple {

  override val componentName: String = componentInfo.name
  var isOnline: Boolean              = false

  def initialize(): Future[Unit]
  def onDomainMsg(msg: Msg): Unit
  def onControlCommand(commandMessage: CommandMessage): Validation
  def onShutdown(): Future[Unit]
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}
