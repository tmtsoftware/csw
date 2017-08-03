package csw.common.framework.scaladsl

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.util.Timeout
import csw.common.components.assembly.AssemblyDomainMsg
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister, HcdInfo}
import csw.common.framework.models.ComponentMsg
import csw.common.framework.scaladsl.assembly.{AssemblyBehaviorFactory, AssemblyHandlers}
import csw.common.framework.scaladsl.hcd.{HcdBehaviorFactory, HcdHandlers}
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

  def getSampleHcdFactory(hcdHandlers: HcdHandlers[HcdDomainMsg]): HcdBehaviorFactory[HcdDomainMsg] =
    new HcdBehaviorFactory[HcdDomainMsg] {
      override def make(ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo): HcdHandlers[HcdDomainMsg] = hcdHandlers
    }

  def getSampleAssemblyFactory(
      assemblyHandlers: AssemblyHandlers[AssemblyDomainMsg]
  ): AssemblyBehaviorFactory[AssemblyDomainMsg] =
    new AssemblyBehaviorFactory[AssemblyDomainMsg] {
      override def make(ctx: ActorContext[ComponentMsg],
                        assemblyInfo: AssemblyInfo): AssemblyHandlers[AssemblyDomainMsg] = assemblyHandlers
    }
}
