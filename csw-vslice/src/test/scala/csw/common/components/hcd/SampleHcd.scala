package csw.common.components.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models._
import csw.common.framework.scaladsl.hcd.HcdActor
import csw.param.Parameters

import scala.concurrent.Future

class SampleHcd(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode])
    extends HcdActor[HcdDomainMessage](ctx, supervisor) {

  var lifeCycleMessageReceived: LifecycleMessageReceived = _

  override def initialize(): Future[Unit] = Future.unit

  override def onRun(): Unit = ()

  override def onShutdown(): Unit = {
    lifeCycleMessageReceived = LifecycleMessageReceived.Shutdown
  }

  override def onRestart(): Unit = {
    lifeCycleMessageReceived = LifecycleMessageReceived.Restart
  }

  override def onRunOnline(): Unit = ()

  override def onGoOffline(): Unit = {
    lifeCycleMessageReceived = LifecycleMessageReceived.RunOffline
  }

  override def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit = ()

  override def onShutdownComplete(): Unit = ()

  override def onSetup(sc: Parameters.Setup): Unit = ()

  override def onDomainMsg(msg: HcdDomainMessage): Unit = msg match {
    case GetCurrentState(replyTo) => replyTo ! HcdDomainResponseMsg(lifeCycleMessageReceived)
  }
}
