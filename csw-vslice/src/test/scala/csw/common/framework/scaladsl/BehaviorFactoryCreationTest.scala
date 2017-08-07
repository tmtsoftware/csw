package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import csw.common.ccs.Validation
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.internal.supervisor.Supervisor
import csw.common.framework.models.Component.{ComponentInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.SupervisorIdleMsg.Initialized
import csw.common.framework.models._
import csw.param.StateVariable
import csw.param.StateVariable.CurrentState
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{Await, Future}

class SampleHcdBehaviorFactory extends ComponentBehaviorFactory[HcdDomainMsg] {
  override def make(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMsg[StateVariable.CurrentState]]
  ): ComponentHandlers[HcdDomainMsg] = new ComponentHandlers[HcdDomainMsg](ctx, componentInfo) {
    override def onRestart(): Unit                                               = println(componentInfo.componentName)
    override def onRun(): Unit                                                   = println(componentInfo.componentName)
    override def onGoOnline(): Unit                                              = println(componentInfo.componentName)
    override def onDomainMsg(msg: HcdDomainMsg): Unit                            = println(componentInfo.componentName)
    override def onShutdown(): Unit                                              = println(componentInfo.componentName)
    override def onControlCommand(commandMsg: CommandMsg): Validation.Validation = Validation.Valid
    override def initialize(): Future[Unit]                                      = Future.unit
    override def onGoOffline(): Unit                                             = println(componentInfo.componentName)
  }
}

class BehaviorFactoryCreationTest extends FrameworkComponentTestSuite with Matchers with MockitoSugar {

  override val hcdInfo = HcdInfo("SampleHcd",
                                 "wfos",
                                 "csw.common.framework.scaladsl.SampleHcdBehaviorFactory",
                                 DoNotRegister,
                                 Set(AkkaType),
                                 FiniteDuration(5, "seconds"))

  private def hcdBehaviorFactory(componentInfo: ComponentInfo): ComponentBehaviorFactory[_] = {
    val componentFactoryClass = Class.forName(componentInfo.componentClassName)
    componentFactoryClass.newInstance().asInstanceOf[ComponentBehaviorFactory[_]]
  }

  test(
    "supervisor should be able to get component behavior factory after it's instance is created using reflection from the component info name"
  ) {
    val testSupervisor = TestProbe[FromComponentLifecycleMessage]
    val testPubSub     = TestProbe[PublisherMsg[CurrentState]]

    val component =
      Await.result(
        system.systemActorOf[Nothing](hcdBehaviorFactory(hcdInfo).behavior(hcdInfo, testSupervisor.ref, testPubSub.ref),
                                      "sampleHcd"),
        5.seconds
      )

    testSupervisor.expectMsgType[Initialized]

  }
}
