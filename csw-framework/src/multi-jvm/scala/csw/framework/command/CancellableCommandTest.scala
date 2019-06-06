package csw.framework.command

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.CommandMessage.{Oneway, Submit}
import csw.common.components.command.ComponentStateForCommand.{acceptedCmd, cancelCmd, prefix}
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.location.models.{ComponentId, ComponentType}
import csw.location.models.Connection.AkkaConnection
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.params.commands.CommandResponse._
import csw.params.commands.Setup
import csw.params.core.generics.KeyType
import csw.params.core.models.ObsId
import io.lettuce.core.RedisClient
import org.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class CancellableCommandTestMultiJvm1 extends CancellableCommandTest(0)

class CancellableCommandTestMultiJvm2 extends CancellableCommandTest(0)

// DEOPSCSW-211 Notification of Interrupted Message
class CancellableCommandTest(ignore: Int)
    extends LSNodeSpec(config = new OneMemberAndSeed, mode = "http")
    with MultiNodeHTTPLocationService
    with MockitoSugar {
  import config._

  implicit val actorSystem: ActorSystem[SpawnProtocol] = system.toTyped.asInstanceOf[ActorSystem[SpawnProtocol]]
  implicit val mat: Materializer                       = ActorMaterializer()
  implicit val ec: ExecutionContext                    = actorSystem.executionContext
  implicit val timeout: Timeout                        = 5.seconds
  implicit val scheduler: Scheduler                    = actorSystem.scheduler

  test("a long running command should be cancellable") {
    runOn(seed) {
      // spawn container having assembly and hcd running in jvm-1
      val wiring       = FrameworkWiring.make(actorSystem, locationService, mock[RedisClient])
      val assemblyConf = ConfigFactory.load("command/commanding_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
    }

    runOn(member) {
      val submitResponseProbe = TestProbe[SubmitResponse]
      val queryResponseProbe  = TestProbe[QueryResponse]
      val onewayResponseProbe = TestProbe[OnewayResponse]
      val obsId               = Some(ObsId("Obs001"))
      val cancelCmdId         = KeyType.StringKey.make("cancelCmdId")

      enterBarrier("spawned")

      // resolve the assembly running on seed
      val assemblyLocF =
        locationService.resolve(AkkaConnection(ComponentId("Monitor_Assembly", ComponentType.Assembly)), 5.seconds)
      val assemblyRef = Await.result(assemblyLocF, 10.seconds).map(_.componentRef).get

      // original command is submit and Cancel command is also submit
      // This returns Started, so it is a long-running and we are free to cancel it
      val originalSetup = Setup(prefix, acceptedCmd, obsId)
      assemblyRef ! Submit(originalSetup, submitResponseProbe.ref)
      var originalResponse = submitResponseProbe.expectMessageType[Started]

      // This is the cancel command that is processed
      val cancelSetup = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalResponse.runId.id)))
      assemblyRef ! Submit(cancelSetup, submitResponseProbe.ref)
      var cancelCompletedResponse = submitResponseProbe.expectMessageType[Completed]


      //TODO FIXME FIXME
      //assemblyRef ! Subscribe(cancelCompletedResponse.runId, submitResponseProbe.ref)
      //submitResponseProbe.expectMessage(Completed(cancelCompletedResponse.commandName, cancelCompletedResponse.runId))

      //assemblyRef ! Subscribe(originalResponse.runId, submitResponseProbe.ref)
      //submitResponseProbe.expectMessage(Cancelled(originalResponse.commandName, originalResponse.runId))

      // original command is submit but Cancel command is oneway
      assemblyRef ! Submit(originalSetup, submitResponseProbe.ref)
      originalResponse = submitResponseProbe.expectMessageType[Started]

      val cancelSetup2 = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalResponse.runId.id)))
      assemblyRef ! Oneway(cancelSetup2, onewayResponseProbe.ref)
      onewayResponseProbe.expectMessageType[Accepted]

      //assemblyRef ! Subscribe(originalResponse.runId, submitResponseProbe.ref)
      //submitResponseProbe.expectMessage(Cancelled(originalResponse.commandName, originalResponse.runId))

      // original command is oneway but Cancel command is submit
      assemblyRef ! Oneway(originalSetup, onewayResponseProbe.ref)
      var originalResponse2 = onewayResponseProbe.expectMessageType[Accepted] // (originalSetup3.runId))

      val cancelSetup3 = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalResponse2.runId.id)))
      assemblyRef ! Submit(cancelSetup3, submitResponseProbe.ref)
      cancelCompletedResponse = submitResponseProbe.expectMessageType[Completed] //(cancelSetup3.runId))

      // Note that this works, even though initial Oneway was not in CRM, if code puts Cancelled for the runId into CRM
      // the subscribe will work
      //assemblyRef ! Query(originalResponse2.runId, queryResponseProbe.ref)
      //queryResponseProbe.expectMessage(Cancelled(originalResponse.commandName, originalResponse2.runId))

      // original command is oneway and Cancel command is also oneway
      assemblyRef ! Oneway(originalSetup, onewayResponseProbe.ref)
      originalResponse2 = onewayResponseProbe.expectMessageType[Accepted]

      val cancelSetup4 = Setup(prefix, cancelCmd, obsId, Set(cancelCmdId.set(originalResponse2.runId.id)))
      assemblyRef ! Oneway(cancelSetup4, onewayResponseProbe.ref)
      onewayResponseProbe.expectMessageType[Accepted]
    }
    enterBarrier("end")
  }
}
