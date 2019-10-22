package csw.framework.command

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.common.components.command.ComponentStateForCommand.{acceptedCmd, cancelCmd, prefix}
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.location.models.{ComponentId, ComponentType}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.params.commands.CommandResponse._
import csw.params.commands.Setup
import csw.params.core.generics.KeyType
import csw.params.core.models.ObsId
import io.lettuce.core.RedisClient
import org.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}

class CancellableCommandTestMultiJvm1 extends CancellableCommandTest(0)

class CancellableCommandTestMultiJvm2 extends CancellableCommandTest(0)

// DEOPSCSW-211 Notification of Interrupted Message
class CancellableCommandTest(ignore: Int)
    extends LSNodeSpec(config = new OneMemberAndSeed, mode = "http")
    with MultiNodeHTTPLocationService
    with MockitoSugar {
  import config._

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = system.toTyped.asInstanceOf[ActorSystem[SpawnProtocol.Command]]
  private implicit val mat: Materializer                       = Materializer(typedSystem)
  private implicit val ec: ExecutionContext                    = actorSystem.executionContext
  private implicit val timeout: Timeout                        = 5.seconds
  private implicit val scheduler: Scheduler                    = actorSystem.scheduler

  test("a long running command should be cancellable") {
    runOn(seed) {
      // spawn container having assembly and hcd running in jvm-1
      val wiring       = FrameworkWiring.make(actorSystem, locationService, mock[RedisClient])
      val assemblyConf = ConfigFactory.load("command/commanding_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
    }

    runOn(member) {
      val obsId         = Some(ObsId("Obs001"))
      val cancelCmdId   = KeyType.StringKey.make("cancelCmdId")
      val cancelCmdName = KeyType.StringKey.make(name = "cancelCmdName")

      enterBarrier("spawned")

      // resolve the assembly running on seed
      val assemblyLocF =
        locationService.resolve(
          AkkaConnection(ComponentId("Monitor_Assembly", ComponentType.Assembly)).of[AkkaLocation],
          5.seconds
        )
      val assemblyLoc: AkkaLocation = Await.result(assemblyLocF, 10.seconds).get

      val assemblyCommandService = CommandServiceFactory.make(assemblyLoc)

      // original command is submit and Cancel command is also submit
      // This returns Started, so it is a long-running and we are free to cancel it
      val originalSetup    = Setup(prefix, acceptedCmd, obsId)
      var originalResponse = Await.result(assemblyCommandService.submit(originalSetup), 5.seconds)
      originalResponse shouldBe a[Started]

      // This is the cancel command that is processed
      val cancelSetup = Setup(
        prefix,
        cancelCmd,
        obsId,
        Set(cancelCmdId.set(originalResponse.runId.id), cancelCmdName.set(originalSetup.commandName.name))
      )
      var cancelAsSubmitResponse = Await.result(assemblyCommandService.submit(cancelSetup), 5.seconds)
      cancelAsSubmitResponse shouldBe a[Completed]
      cancelAsSubmitResponse.commandName shouldBe cancelSetup.commandName

      var waitForCancel                        = assemblyCommandService.queryFinal(originalResponse.runId)
      var originalFinalResponse: QueryResponse = Await.result(waitForCancel, 5.seconds)
      originalFinalResponse shouldBe a[Cancelled]
      originalFinalResponse.runId shouldEqual originalResponse.runId
      originalFinalResponse.commandName shouldEqual originalResponse.commandName

      // original command is submit but Cancel command is oneway
      originalResponse = Await.result(assemblyCommandService.submit(originalSetup), 5.seconds)
      originalResponse shouldBe a[Started]

      val cancelSetup2 = Setup(
        prefix,
        cancelCmd,
        obsId,
        Set(cancelCmdId.set(originalResponse.runId.id), cancelCmdName.set(originalSetup.commandName.name))
      )
      var cancelAsOnewayResponse = Await.result(assemblyCommandService.oneway(cancelSetup2), 5.seconds)
      cancelAsOnewayResponse shouldBe a[Accepted]
      cancelAsOnewayResponse.commandName shouldBe cancelSetup2.commandName

      waitForCancel = assemblyCommandService.queryFinal(originalResponse.runId)
      originalFinalResponse = Await.result(waitForCancel, 5.seconds)
      originalFinalResponse shouldBe a[Cancelled]
      originalFinalResponse.runId shouldEqual originalResponse.runId
      originalFinalResponse.commandName shouldEqual originalResponse.commandName

      // original command is oneway but Cancel command is submit
      // Here a oneway can only be Accepted
      var originalResponse2 = Await.result(assemblyCommandService.oneway(originalSetup), 5.seconds)
      originalResponse2 shouldBe a[Accepted]

      cancelAsSubmitResponse = Await.result(assemblyCommandService.submit(cancelSetup), 5.seconds)
      cancelAsSubmitResponse shouldBe a[Completed]
      cancelAsSubmitResponse.commandName shouldBe cancelSetup.commandName

      // Note, even though initial was a oneway, with this approach a cancel can be used
      // as long as original command updates with cancelled. This was same as commandResponseManager
      waitForCancel = assemblyCommandService.queryFinal(originalResponse.runId)
      originalFinalResponse = Await.result(waitForCancel, 5.seconds)
      originalFinalResponse shouldBe a[Cancelled]
      originalFinalResponse.runId shouldEqual originalResponse.runId
      originalFinalResponse.commandName shouldEqual originalResponse.commandName

      // original command is oneway and Cancel command is also oneway
      originalResponse2 = Await.result(assemblyCommandService.oneway(originalSetup), 5.seconds)
      originalResponse2 shouldBe a[Accepted]

      cancelAsOnewayResponse = Await.result(assemblyCommandService.oneway(cancelSetup2), 5.seconds)
      cancelAsOnewayResponse shouldBe a[Accepted]
      cancelAsOnewayResponse.commandName shouldBe cancelSetup2.commandName

      // Note, even though initial was a oneway, with this approach a cancel can be used
      // as long as original command updates with cancelled. This was same as commandResponseManager
      waitForCancel = assemblyCommandService.queryFinal(originalResponse.runId)
      originalFinalResponse = Await.result(waitForCancel, 5.seconds)
      originalFinalResponse shouldBe a[Cancelled]
      originalFinalResponse.runId shouldEqual originalResponse.runId
      originalFinalResponse.commandName shouldEqual originalResponse.commandName
    }
    enterBarrier("end")
  }
}
