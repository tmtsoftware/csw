package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.assembly.SampleAssembly
import csw.common.framework.models.{AssemblyComponentLifecycleMessage, AssemblyMsg}
import csw.common.framework.models.AssemblyComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister}
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.services.location.models.ConnectionType.AkkaType
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AssemblyActorTest extends FunSuite with Matchers {

  test("Assembly actor sends Initialized and Running message to Supervisor") {

    implicit val system   = ActorSystem("actor-system", Actor.empty)
    implicit val settings = TestKitSettings(system)
    implicit val timeout  = Timeout(5.seconds)

    val supervisorProbe: TestProbe[AssemblyComponentLifecycleMessage] = TestProbe[AssemblyComponentLifecycleMessage]

    val assemblyInfo =
      AssemblyInfo("trombone",
                   "wfos",
                   "csw.common.components.assembly.SampleAssembly",
                   DoNotRegister,
                   Set(AkkaType),
                   Set.empty)

    val assemblyRef =
      Await.result(
        system.systemActorOf[AssemblyMsg](SampleAssembly.behaviour(assemblyInfo, supervisorProbe.ref), "assembly"),
        5.seconds
      )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.assemblyRef shouldBe assemblyRef

    initialized.assemblyRef ! Run(supervisorProbe.ref)

    val running = supervisorProbe.expectMsgType[Running]
    running.assemblyRef shouldBe assemblyRef
  }
}
