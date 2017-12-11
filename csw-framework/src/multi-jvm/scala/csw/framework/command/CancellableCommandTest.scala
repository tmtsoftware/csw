package csw.framework.command

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.components.command.ComponentStateForCommand.{acceptedCmdPrefix, cancelCmdPrefix, prefix}
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage.Subscribe
import csw.messages.ccs.commands.CommandResponse.{Accepted, Cancelled, Completed}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.params.generics.KeyType
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
      val cmdResponseProbe = TestProbe[CommandResponse]
      val obsId            = Some(ObsId("Obs001"))
      val cancelCmdId      = KeyType.StringKey.make("cancelCmdId")

      enterBarrier("spawned")

      // resolve the assembly running on seed
      val assemblyLocF =
        locationService.resolve(AkkaConnection(ComponentId("Monitor_Assembly", ComponentType.Assembly)), 5.seconds)
      val assemblyRef = Await.result(assemblyLocF, 10.seconds).map(_.componentRef()).get

      // original command is submit and Cancel command is also submit
      val originalSetup = Setup(prefix, acceptedCmdPrefix, obsId)
      assemblyRef ! Submit(originalSetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(originalSetup.runId))

      val cancelSetup = Setup(prefix, cancelCmdPrefix, obsId, Set(cancelCmdId.set(originalSetup.runId.id)))
      assemblyRef ! Submit(cancelSetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup.runId))

      assemblyRef ! Subscribe(cancelSetup.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Completed(cancelSetup.runId))

      assemblyRef ! Subscribe(originalSetup.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Cancelled(originalSetup.runId))

      // original command is submit but Cancel command is oneway
      val originalSetup2 = Setup(prefix, acceptedCmdPrefix, obsId)
      assemblyRef ! Submit(originalSetup2, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(originalSetup2.runId))

      val cancelSetup2 = Setup(prefix, cancelCmdPrefix, obsId, Set(cancelCmdId.set(originalSetup2.runId.id)))
      assemblyRef ! Oneway(cancelSetup2, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup2.runId))

      assemblyRef ! Subscribe(originalSetup2.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Cancelled(originalSetup2.runId))

      // original command is oneway but Cancel command is submit
      val originalSetup3 = Setup(prefix, acceptedCmdPrefix, obsId)
      assemblyRef ! Oneway(originalSetup3, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(originalSetup3.runId))

      val cancelSetup3 = Setup(prefix, cancelCmdPrefix, obsId, Set(cancelCmdId.set(originalSetup3.runId.id)))
      assemblyRef ! Submit(cancelSetup3, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup3.runId))

      assemblyRef ! Subscribe(cancelSetup3.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Completed(cancelSetup3.runId))

      // original command is oneway and Cancel command is also oneway
      val originalSetup4 = Setup(prefix, acceptedCmdPrefix, obsId)
      assemblyRef ! Oneway(originalSetup4, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(originalSetup4.runId))

      val cancelSetup4 = Setup(prefix, cancelCmdPrefix, obsId, Set(cancelCmdId.set(originalSetup4.runId.id)))
      assemblyRef ! Oneway(cancelSetup4, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(Accepted(cancelSetup4.runId))
    }
    enterBarrier("end")
  }
}
