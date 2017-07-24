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
import csw.common.framework.models._
import csw.common.framework.scaladsl.{HcdActor, HcdActorFactory}
import csw.param.Parameters

import scala.concurrent.Future

class TestHcdFactory extends HcdActorFactory[TestHcdMessage] {
  override def make(ctx: ActorContext[HcdMsg],
                    supervisor: ActorRef[HcdComponentLifecycleMessage]): HcdActor[TestHcdMessage] =
    new TestHcd(ctx, supervisor)
}

class TestHcd(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends HcdActor[TestHcdMessage](ctx, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onInitialRun(): Unit = ()

  override def onRunningHcdShutdownComplete(): Unit = ()

  override def onSetup(sc: Parameters.Setup): Unit = ()

  override def onDomainMsg(msg: TestHcdMessage): Unit = ()

  override def onInitialHcdShutdownComplete(): Unit = ()

  override def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown                            => supervisor ! ShutdownComplete
    case Restart                             => init.map(_ ⇒ ())
    case Run                                 =>
    case RunOffline                          =>
    case LifecycleFailureInfo(state, reason) =>
    case ShutdownComplete                    =>
  }
}
