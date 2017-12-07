package csw.framework.command

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.components.command.ComponentStateForCommand.{cancelCmdPrefix, cancellableCmdPrefix}
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage.Subscribe
import csw.messages.ccs.commands.CommandResponse.{Accepted, Cancelled, Completed}
import csw.messages.ccs.commands.{Cancel, CommandResponse, Setup}
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.models.ObsId
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class CancellableCommandTestMultiJvm1 extends CancellableCommandTest(0)
class CancellableCommandTestMultiJvm2 extends CancellableCommandTest(0)

// DEOPSCSW-211 Notification of Interrupted Message
class CancellableCommandTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) {
  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val mat: Materializer           = ActorMaterializer()
  implicit val ec: ExecutionContext        = actorSystem.executionContext
  implicit val timeout: Timeout            = 5.seconds
  implicit val scheduler: Scheduler        = actorSystem.scheduler
  implicit val testkit: TestKitSettings    = TestKitSettings(actorSystem)

  test("a long running command should be cancellable") {
    runOn(seed) {
      // spawn container having assembly and hcd running in jvm-1
      val wiring       = FrameworkWiring.make(system, locationService)
      val assemblyConf = ConfigFactory.load("command/commanding_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
    }

    runOn(member) {
      val obsId            = Some(ObsId("Obs001"))
      val cmdResponseProbe = TestProbe[CommandResponse]

      enterBarrier("spawned")

      // resolve the assembly running on seed
      val assemblyLocF =
        locationService.resolve(AkkaConnection(ComponentId("Monitor_Assembly", ComponentType.Assembly)), 5.seconds)
      val assemblyRef = Await.result(assemblyLocF, 10.seconds).map(_.componentRef()).get

      // original command is submit and Cancel command is also submit
      val cancellableSetup = Setup(cancellableCmdPrefix, obsId)
      assemblyRef ! Submit(cancellableSetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancellableSetup.runId))

      val cancelSetup = Cancel(cancelCmdPrefix, obsId, cancellableSetup.runId)
      assemblyRef ! Submit(cancelSetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup.runId))

      assemblyRef ! Subscribe(cancelSetup.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Completed(cancelSetup.runId))

      assemblyRef ! Subscribe(cancellableSetup.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Cancelled(cancellableSetup.runId))

      // original command is submit but Cancel command is oneway
      val cancellableSetup2 = Setup(cancellableCmdPrefix, obsId)
      assemblyRef ! Submit(cancellableSetup2, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancellableSetup2.runId))

      val cancelSetup2 = Cancel(cancelCmdPrefix, obsId, cancellableSetup2.runId)
      assemblyRef ! Oneway(cancelSetup2, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup2.runId))

      assemblyRef ! Subscribe(cancellableSetup2.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Cancelled(cancellableSetup2.runId))

      // original command is oneway but Cancel command is submit
      val cancellableSetup3 = Setup(cancellableCmdPrefix, obsId)
      assemblyRef ! Oneway(cancellableSetup3, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancellableSetup3.runId))

      val cancelSetup3 = Cancel(cancelCmdPrefix, obsId, cancellableSetup3.runId)
      assemblyRef ! Submit(cancelSetup3, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup3.runId))

      assemblyRef ! Subscribe(cancelSetup3.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Completed(cancelSetup3.runId))

      // original command is oneway and Cancel command is also oneway
      val cancellableSetup4 = Setup(cancellableCmdPrefix, obsId)
      assemblyRef ! Oneway(cancellableSetup4, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancellableSetup4.runId))

      val cancelSetup4 = Cancel(cancelCmdPrefix, obsId, cancellableSetup4.runId)
      assemblyRef ! Oneway(cancelSetup4, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup4.runId))
    }
    enterBarrier("end")
  }
}
