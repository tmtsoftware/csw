package csw.common.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.ccs.Validation
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.Component.{ComponentInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.Shutdown
import csw.common.framework.models._
import csw.param.states.CurrentState
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{Await, Future}

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

class CompWiringTest extends FrameworkComponentTestSuite with Matchers with MockitoSugar {

  override val hcdInfo = HcdInfo("SampleHcd",
                                 "wfos",
                                 "csw.common.framework.scaladsl.SampleHcdWiring",
                                 DoNotRegister,
                                 Set(AkkaType),
                                 FiniteDuration(5, "seconds"))

  test(
    "supervisor should be able to get component behavior factory after it's instance is created using reflection from the component info name"
  ) {
    val testSupervisor = TestProbe[FromComponentLifecycleMessage]

    val supervisor =
      Await.result(system.systemActorOf(SupervisorBehaviorFactory.make(hcdInfo), "sampleHcd"), 5.seconds)

    Thread.sleep(1000)
    supervisor ! Lifecycle(Shutdown)
  }
}
