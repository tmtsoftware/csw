package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.common.components.assembly.AssemblyDomainMsg
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.Component.{AssemblyInfo, ComponentInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.ComponentMsg
import csw.common.framework.models.PubSub.PublisherMsg
import csw.param.commons.CurrentState
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationLong, FiniteDuration}

abstract class FrameworkComponentTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

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
                        "csw.common.components.hcd.SampleHcd",
                        DoNotRegister,
                        Set(AkkaType),
                        FiniteDuration(5, "seconds"))

  def getSampleHcdFactory(componentHandlers: ComponentHandlers[HcdDomainMsg]): ComponentBehaviorFactory[HcdDomainMsg] =
    new ComponentBehaviorFactory[HcdDomainMsg] {

      override def make(ctx: ActorContext[ComponentMsg],
                        componentInfo: ComponentInfo,
                        pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[HcdDomainMsg] =
        componentHandlers
    }

  def getSampleAssemblyFactory(
      assemblyHandlers: ComponentHandlers[AssemblyDomainMsg]
  ): ComponentBehaviorFactory[AssemblyDomainMsg] =
    new ComponentBehaviorFactory[AssemblyDomainMsg] {
      override def make(ctx: ActorContext[ComponentMsg],
                        componentInfo: ComponentInfo,
                        pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[AssemblyDomainMsg] =
        assemblyHandlers
    }

}
