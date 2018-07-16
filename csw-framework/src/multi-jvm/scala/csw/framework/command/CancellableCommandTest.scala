package csw.framework.command

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.components.command.ComponentStateForCommand.{acceptedCmd, cancelCmd, prefix}
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage.Subscribe
import csw.messages.commands.CommandResponse.{Cancelled, Completed}
import csw.messages.commands.ValidationResponse.Accepted
import csw.messages.commands.{CommandResponseBase, Setup}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.params.generics.KeyType
import csw.messages.params.models.ObsId
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.params.commands.CommandResponse.{Accepted, Cancelled, Completed}
import csw.params.commands.{CommandResponse, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.ObsId
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class CancellableCommandTestMultiJvm1 extends CancellableCommandTest(0)
class CancellableCommandTestMultiJvm2 extends CancellableCommandTest(0)

// DEOPSCSW-211 Notification of Interrupted Message
class CancellableCommandTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) with MockitoSugar {
  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val mat: Materializer           = ActorMaterializer()
  implicit val ec: ExecutionContext        = actorSystem.executionContext
  implicit val timeout: Timeout            = 5.seconds
  implicit val scheduler: Scheduler        = actorSystem.scheduler

  test("a long running command should be cancellable") {
    runOn(seed) {
      // spawn container having assembly and hcd running in jvm-1
      val wiring       = FrameworkWiring.make(system, locationService, mock[RedisClient])
      val assemblyConf = ConfigFactory.load("command/commanding_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
    }

    runOn(member) {
      val cmdResponseProbe = TestProbe[CommandResponseBase]
      val obsId            = Some(ObsId("Obs001"))
      val cancelCmdId      = KeyType.StringKey.make("cancelCmdId")

      enterBarrier("spawned")

      // resolve the assembly running on seed
      val assemblyLocF =
        locationService.resolve(AkkaConnection(ComponentId("Monitor_Assembly", ComponentType.Assembly)), 5.seconds)
      val assemblyRef = Await.result(assemblyLocF, 10.seconds).map(_.componentRef).get

      // original command is submit and Cancel command is also submit
      val originalSetup = Setup(prefix, acceptedCmd, obsId)
      assemblyRef ! Submit(originalSetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(originalSetup.runId))

      val cancelSetup = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalSetup.runId.id)))
      assemblyRef ! Submit(cancelSetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(cancelSetup.runId))

      assemblyRef ! Subscribe(cancelSetup.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Completed(cancelSetup.runId))

      assemblyRef ! Subscribe(originalSetup.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Cancelled(originalSetup.runId))

      // original command is submit but Cancel command is oneway
      val originalSetup2 = Setup(prefix, acceptedCmd, obsId)
      assemblyRef ! Submit(originalSetup2, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(originalSetup2.runId))

      val cancelSetup2 = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalSetup2.runId.id)))
      assemblyRef ! Oneway(cancelSetup2, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(cancelSetup2.runId))

      assemblyRef ! Subscribe(originalSetup2.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Cancelled(originalSetup2.runId))

      // original command is oneway but Cancel command is submit
      val originalSetup3 = Setup(prefix, acceptedCmd, obsId)
      assemblyRef ! Oneway(originalSetup3, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(originalSetup3.runId))

      val cancelSetup3 = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalSetup3.runId.id)))
      assemblyRef ! Submit(cancelSetup3, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(cancelSetup3.runId))

      assemblyRef ! Subscribe(cancelSetup3.runId, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Completed(cancelSetup3.runId))

      // original command is oneway and Cancel command is also oneway
      val originalSetup4 = Setup(prefix, acceptedCmd, obsId)
      assemblyRef ! Oneway(originalSetup4, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(originalSetup4.runId))

      val cancelSetup4 = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalSetup4.runId.id)))
      assemblyRef ! Oneway(cancelSetup4, cmdResponseProbe.ref)
      cmdResponseProbe.expectMessage(Accepted(cancelSetup4.runId))
    }
    enterBarrier("end")
  }
}
