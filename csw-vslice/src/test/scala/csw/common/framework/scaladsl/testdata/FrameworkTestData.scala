package csw.common.framework.scaladsl.testdata

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.Validation
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.Component.{ComponentInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.{CommandMsg, ComponentMsg, PubSub}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState
import csw.services.location.models.ConnectionType.AkkaType

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SampleHcdHandlers(ctx: ActorContext[ComponentMsg], componentInfo: ComponentInfo)
    extends ComponentHandlers[HcdDomainMsg](ctx, componentInfo) {
  override def onRestart(): Unit                                               = println(s"${componentInfo.componentName} restarting")
  override def onRun(): Unit                                                   = println(s"${componentInfo.componentName} running")
  override def onGoOnline(): Unit                                              = println(s"${componentInfo.componentName} going online")
  override def onDomainMsg(msg: HcdDomainMsg): Unit                            = println(s"${componentInfo.componentName} going offline")
  override def onShutdown(): Unit                                              = println(s"${componentInfo.componentName} shutting down")
  override def onControlCommand(commandMsg: CommandMsg): Validation.Validation = Validation.Valid
  override def initialize(): Future[Unit]                                      = Future.unit
  override def onGoOffline(): Unit                                             = println(s"${componentInfo.componentName} going offline")
}

class SampleHcdWiring extends ComponentWiring[HcdDomainMsg] {
  override def handlers(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]
  ): ComponentHandlers[HcdDomainMsg] = new SampleHcdHandlers(ctx, componentInfo)
}

object FrameworkTestData {
  val hcdInfo = HcdInfo("SampleHcd",
                        "wfos",
                        "csw.common.framework.scaladsl.testdata.SampleHcdWiring",
                        DoNotRegister,
                        Set(AkkaType),
                        FiniteDuration(5, "seconds"))
}
