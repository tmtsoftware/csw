package csw.common.framework.scaladsl

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.util.Timeout
import csw.common.components.assembly.AssemblyDomainMessages
import csw.common.components.hcd.HcdDomainMessage
import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.{AssemblyMsg, HcdMsg}
import csw.common.framework.scaladsl.assembly.{AssemblyHandlers, AssemblyHandlersFactory}
import csw.common.framework.scaladsl.hcd.{HcdHandlers, HcdHandlersFactory}
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

  def getSampleHcdFactory(hcdHandlers: HcdHandlers[HcdDomainMessage]): HcdHandlersFactory[HcdDomainMessage] =
    new HcdHandlersFactory[HcdDomainMessage] {
      override def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): HcdHandlers[HcdDomainMessage] = hcdHandlers
    }

  def getSampleAssemblyFactory(
      assemblyHandlers: AssemblyHandlers[AssemblyDomainMessages]
  ): AssemblyHandlersFactory[AssemblyDomainMessages] =
    new AssemblyHandlersFactory[AssemblyDomainMessages] {
      override def make(ctx: ActorContext[AssemblyMsg],
                        assemblyInfo: AssemblyInfo): AssemblyHandlers[AssemblyDomainMessages] = assemblyHandlers
    }
}
