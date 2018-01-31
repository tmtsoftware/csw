package csw.framework.command

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed}
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, Setup}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.models.ObsId
import csw.messages.params.states.CurrentState
import csw.services.ccs.scaladsl.CommandDistributor
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class LongRunningCommandTestMultiJvm1 extends LongRunningCommandTest(0)
class LongRunningCommandTestMultiJvm2 extends LongRunningCommandTest(0)
class LongRunningCommandTestMultiJvm3 extends LongRunningCommandTest(0)

// DEOPSCSW-194: Support long running actions asynchronously
// DEOPSCSW-227: Distribute commands to multiple destinations
// DEOPSCSW-228: Assist Components with command completion
class LongRunningCommandTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) with ScalaFutures {
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
      val assemblyComponent: ComponentRef = Await.result(assemblyLocF, 5.seconds).map(_.component).get

      val setup = Setup(prefix, longRunning, Some(obsId))
      val probe = TestProbe[CurrentState]
      assemblyComponent.value ! ComponentStateSubscription(Subscribe(probe.ref))

      // send submit with setup to assembly running in JVM-2
      // then assembly will split it into three sub commands [McsAssemblyComponentHandlers]
      // assembly will forward this sub commands to hcd in following sequence
      // 1. longSetup which takes 5 seconds to finish
      // 2. shortSetup which takes 1 second to finish
      // 3. mediumSetup which takes 3 seconds to finish

      //#subscribe-for-result
      val eventualCommandResponse = assemblyComponent.submit(setup).flatMap {
        case _: Accepted ⇒ assemblyComponent.subscribe(setup.runId)
        case _           ⇒ Future(CommandResponse.Error(setup.runId, ""))
      }
      //#subscribe-for-result

      Await.result(eventualCommandResponse, 20.seconds) shouldBe Completed(setup.runId)

      //#submitAndSubscribe
      val response = assemblyComponent.submitAndSubscribe(setup)
      //#submitAndSubscribe

      Await.result(response, 20.seconds) shouldBe Completed(setup.runId)

      //#query-response
      assemblyComponent.submit(setup)

      // do some work before querying for the result of above command as needed

      val eventualResponse: Future[CommandResponse] = assemblyComponent.query(setup.runId)
      //#query-response

      // verify that commands gets completed in following sequence
      // ShortSetup => MediumSetup => LongSetup
      probe.expectMsg(CurrentState(prefix, Set(choiceKey.set(shortCmdCompleted))))
      probe.expectMsg(CurrentState(prefix, Set(choiceKey.set(mediumCmdCompleted))))
      probe.expectMsg(CurrentState(prefix, Set(choiceKey.set(longCmdCompleted))))
      enterBarrier("long-commands")

      val hcdLocF =
        locationService.resolve(
          AkkaConnection(ComponentId("Test_Component_Running_Long_Command", ComponentType.HCD)),
          5.seconds
        )
      val hcdComponent: ComponentRef = Await.result(hcdLocF, 5.seconds).map(_.component).get

      val setupAssembly1 = Setup(prefix, moveCmd, Some(obsId))
      val setupAssembly2 = Setup(prefix, initCmd, Some(obsId))
      val setupHcd1      = Setup(prefix, shortRunning, Some(obsId))
      val setupHcd2      = Setup(prefix, mediumRunning, Some(obsId))

      //#aggregated-validation
      val aggregatedValidationResponse = CommandDistributor(
        Map(assemblyComponent → Set(setupAssembly1, setupAssembly2), hcdComponent → Set(setupHcd1, setupHcd2))
      ).aggregatedValidationResponse()
      //#aggregated-validation

      whenReady(aggregatedValidationResponse, PatienceConfiguration.Timeout(20.seconds)) { result ⇒
        result shouldBe a[Accepted]
      }

      enterBarrier("multiple-components-submit-multiple-commands")

      //#aggregated-completion
      val aggregatedResponse = CommandDistributor(
        Map(assemblyComponent → Set(setupAssembly1, setupAssembly2), hcdComponent → Set(setupHcd1, setupHcd2))
      ).aggregatedCompletionResponse()
      //#aggregated-completion

      whenReady(aggregatedResponse, PatienceConfiguration.Timeout(20.seconds)) { result ⇒
        result shouldBe a[Completed]
      }

      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }

    runOn(member1) {
      // spawn single assembly running in Standalone mode in jvm-2
      val wiring       = FrameworkWiring.make(system, locationService)
      val assemblyConf = ConfigFactory.load("command/mcs_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")
      enterBarrier("multiple-components-submit-multiple-commands")
      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }

    runOn(member2) {
      // spawn single hcd running in Standalone mode in jvm-3
      val wiring  = FrameworkWiring.make(system, locationService)
      val hcdConf = ConfigFactory.load("command/mcs_hcd.conf")
      Await.result(Standalone.spawn(hcdConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")
      enterBarrier("multiple-components-submit-multiple-commands")
      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }
    enterBarrier("end")
  }
}
