package csw.framework.javadsl.command

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.params.models.ObsId
import csw.services.ccs.common.ActorRefExts.RichComponentActor
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class LongRunningCommandTestMultiJvm1 extends LongRunningCommandTest(0)
class LongRunningCommandTestMultiJvm2 extends LongRunningCommandTest(0)
class LongRunningCommandTestMultiJvm3 extends LongRunningCommandTest(0)

class LongRunningCommandTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {
  import config._

  implicit val actorSystem: ActorSystem[_]  = system.toTyped
  implicit val mat: Materializer            = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout             = 20.seconds
  implicit val scheduler: Scheduler         = actorSystem.scheduler
  implicit val testkit: TestKitSettings     = TestKitSettings(actorSystem)

  test("should be able to send long running commands asynchronously and get the response") {
    runOn(seed) {
      // cluster seed is running on jvm-1
      enterBarrier("spawned")
      val obsId = ObsId("Obs001")

      // resolve assembly running in jvm-2 and send setup command expecting immediate command completion response
      val assemblyLocF =
        locationService.resolve(
          AkkaConnection(ComponentId("Test_Component_Running_Long_Command", ComponentType.Assembly)),
          5.seconds
        )
      val assemblyRef = Await.result(assemblyLocF, 5.seconds).map(_.componentRef()).get

      val setup = Setup(longRunningCmdPrefix, Some(obsId))

      val eventualCommandResponse = assemblyRef.submit(setup).flatMap {
        case _: Accepted ⇒ assemblyRef.getCommandResponse(setup.runId)
        case _           ⇒ Future(CommandResponse.Error(setup.runId, ""))
      }

      Await.result(eventualCommandResponse, 20.seconds) shouldBe Completed(setup.runId)

      enterBarrier("long-commands")

    }

    runOn(member1) {

      // spawn single assembly running in Standalone mode in jvm-2
      val wiring       = FrameworkWiring.make(system, locationService)
      val assemblyConf = ConfigFactory.load("command/mcs_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")

    }

    runOn(member2) {

      // spawn single hcd running in Standalone mode in jvm-3
      val wiring  = FrameworkWiring.make(system, locationService)
      val hcdConf = ConfigFactory.load("command/mcs_hcd.conf")
      Await.result(Standalone.spawn(hcdConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")

    }

    enterBarrier("end")
  }
}
