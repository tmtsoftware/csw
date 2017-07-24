package csw.common.components.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.ToComponentLifecycleMessage.{
  LifecycleFailureInfo,
  Restart,
  Run,
  RunOffline,
  Shutdown
}
import csw.common.framework.models.{HcdComponentLifecycleMessage, HcdMsg, ShutdownComplete, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.HcdActor
import csw.param.Parameters

import scala.concurrent.Future

class SampleHcd(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends HcdActor[HcdDomainMessage](ctx, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onInitialRun(): Unit = ()

  override def onRunningHcdShutdownComplete(): Unit = ()

  override def onInitialHcdShutdownComplete(): Unit = ()

  override def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown                            => supervisor ! ShutdownComplete
    case Restart                             => init.map(_ ⇒ ())
    case Run                                 =>
    case RunOffline                          =>
    case LifecycleFailureInfo(state, reason) =>
    case ShutdownComplete                    =>
  }

  override def onSetup(sc: Parameters.Setup): Unit = ()

  override def onDomainMsg(msg: HcdDomainMessage): Unit = ()
}
