package csw.common.framework.scaladsl

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.{Validation, Validations}
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.ComponentInfo.HcdInfo
import csw.common.framework.models.LocationServiceUsage.DoNotRegister
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.Shutdown
import csw.common.framework.models._
import csw.param.states.CurrentState
import csw.services.location.models.ConnectionType.AkkaType

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SampleCompHandlers(ctx: ActorContext[ComponentMsg], componentInfo: ComponentInfo)
    extends ComponentHandlers[HcdDomainMsg](ctx, componentInfo) {
  override def onRestart(): Unit                                    = println(s"${componentInfo.componentName} restarting")
  override def onRun(): Unit                                        = println(s"${componentInfo.componentName} running")
  override def onGoOnline(): Unit                                   = println(s"${componentInfo.componentName} going online")
  override def onDomainMsg(msg: HcdDomainMsg): Unit                 = println(s"${componentInfo.componentName} going offline")
  override def onShutdown(): Unit                                   = println(s"${componentInfo.componentName} shutting down")
  override def onControlCommand(commandMsg: CommandMsg): Validation = Validations.Valid
  override def initialize(): Future[Unit]                           = Future.unit
  override def onGoOffline(): Unit                                  = println(s"${componentInfo.componentName} going offline")
}

class SampleCompWiring extends ComponentWiring[HcdDomainMsg] {
  override def handlers(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]
  ): ComponentHandlers[HcdDomainMsg] = new SampleHcdHandlers(ctx, componentInfo)
}

object SampleSupervisor extends App {

  val hcdInfo = HcdInfo("SampleHcd",
                        "wfos",
                        "csw.common.framework.scaladsl.SampleHcdWiring",
                        DoNotRegister,
                        Set(AkkaType),
                        FiniteDuration(5, "seconds"))

  // The top level actor system has to be untyped so that it can co-host both typed and untyped actors
  val system = ActorSystem("test")
  import akka.typed.scaladsl.adapter._

  val supervisor = system.spawn(SupervisorBehaviorFactory.make(hcdInfo), hcdInfo.componentClassName)

  supervisor ! Lifecycle(Shutdown)
}
