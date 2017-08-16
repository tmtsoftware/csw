package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.common.ccs.{Validation, Validations}
import csw.common.components.assembly.AssemblyDomainMsg
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.ComponentInfo.{AssemblyInfo, HcdInfo}
import csw.common.framework.models.LocationServiceUsages.DoNotRegister
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.{CommandMsg, ComponentInfo, ComponentMsg, PubSub}
import csw.param.states.CurrentState
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

class SampleHcdHandlers(ctx: ActorContext[ComponentMsg],
                        componentInfo: ComponentInfo,
                        pubSubRef: ActorRef[PublisherMsg[CurrentState]])
    extends ComponentHandlers[HcdDomainMsg](ctx, componentInfo, pubSubRef) {
  override def onRestart(): Unit                                    = println(s"${componentInfo.componentName} restarting")
  override def onRun(): Unit                                        = println(s"${componentInfo.componentName} running")
  override def onGoOnline(): Unit                                   = println(s"${componentInfo.componentName} going online")
  override def onDomainMsg(msg: HcdDomainMsg): Unit                 = println(s"${componentInfo.componentName} going offline")
  override def onShutdown(): Unit                                   = println(s"${componentInfo.componentName} shutting down")
  override def onControlCommand(commandMsg: CommandMsg): Validation = Validations.Valid
  override def initialize(): Future[Unit]                           = Future.unit
  override def onGoOffline(): Unit                                  = println(s"${componentInfo.componentName} going offline")
}

class SampleHcdWiring extends ComponentWiring[HcdDomainMsg] {
  override def handlers(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]
  ): ComponentHandlers[HcdDomainMsg] = new SampleHcdHandlers(ctx, componentInfo, pubSubRef)
}

abstract class FrameworkComponentTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Actor.empty, "testHcd")
  implicit val settings: TestKitSettings    = TestKitSettings(system)
  implicit val timeout: Timeout             = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 5.seconds)
  }

  val assemblyInfo = AssemblyInfo("trombone",
                                  "wfos",
                                  "csw.common.components.assembly.SampleAssembly",
                                  DoNotRegister,
                                  Set(AkkaType),
                                  Set.empty)

  val hcdInfo = HcdInfo("SampleHcd",
                        "wfos",
                        "csw.common.framework.scaladsl.SampleHcdWiring",
                        DoNotRegister,
                        Set(AkkaType),
                        FiniteDuration(5, "seconds"))

  def getSampleHcdWiring(componentHandlers: ComponentHandlers[HcdDomainMsg]): ComponentWiring[HcdDomainMsg] =
    new ComponentWiring[HcdDomainMsg] {

      override def handlers(ctx: ActorContext[ComponentMsg],
                            componentInfo: ComponentInfo,
                            pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[HcdDomainMsg] =
        componentHandlers
    }

  def getSampleAssemblyWiring(
      assemblyHandlers: ComponentHandlers[AssemblyDomainMsg]
  ): ComponentWiring[AssemblyDomainMsg] =
    new ComponentWiring[AssemblyDomainMsg] {
      override def handlers(ctx: ActorContext[ComponentMsg],
                            componentInfo: ComponentInfo,
                            pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[AssemblyDomainMsg] =
        assemblyHandlers
    }

}
