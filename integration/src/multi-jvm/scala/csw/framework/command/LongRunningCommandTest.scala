package csw.framework.command

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.Scheduler
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}
import io.lettuce.core.RedisClient
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class LongRunningCommandTestMultiJvm1 extends LongRunningCommandTest(0)
class LongRunningCommandTestMultiJvm2 extends LongRunningCommandTest(0)
class LongRunningCommandTestMultiJvm3 extends LongRunningCommandTest(0)

// DEOPSCSW-194: Support long running actions asynchronously
// DEOPSCSW-227: Distribute commands to multiple destinations
// DEOPSCSW-228: Assist Components with command completion
// DEOPSCSW-233: Hide implementation by having a CCS API
// CSW-82: ComponentInfo should take prefix
class LongRunningCommandTest(ignore: Int)
    extends LSNodeSpec(config = new TwoMembersAndSeed, mode = "http")
    with MultiNodeHTTPLocationService
    with ScalaFutures
    with MockitoSugar {
  import config._

  implicit val ec: ExecutionContextExecutor = typedSystem.executionContext
  implicit val timeout: Timeout             = 20.seconds
  implicit val scheduler: Scheduler         = typedSystem.scheduler
  implicit val testkit: TestKitSettings     = TestKitSettings(typedSystem)

  test(
    "should be able to send long running commands asynchronously and get the response | DEOPSCSW-194, DEOPSCSW-227, DEOPSCSW-228, DEOPSCSW-233"
  ) {
    runOn(seed) {
      // cluster seed is running on jvm-1
      enterBarrier("spawned")
      val obsId = ObsId("2020A-P001-O123")

      // resolve assembly running in jvm-2 and send setup command expecting immediate command completion response
      val assemblyLocF =
        locationService.resolve(
          AkkaConnection(ComponentId(Prefix(Subsystem.IRIS, "Test_Component_Running_Long_Command"), ComponentType.Assembly)),
          5.seconds
        )
      val assemblyLocation: AkkaLocation = Await.result(assemblyLocF, 10.seconds).get
      val assemblyCommandService         = CommandServiceFactory.make(assemblyLocation)

      val assemblyLongSetup = Setup(prefix, longRunning, Some(obsId))
      val probe             = TestProbe[CurrentState]()

      //#subscribeCurrentState
      // subscribe to the current state of an assembly component and use a callback which forwards each received
      // element to a test probe actor
      assemblyCommandService.subscribeCurrentState(probe.ref ! _)
      //#subscribeCurrentState

      // assemblyLongSetup does the following:
      // send submit with setup to assembly running in JVM-2
      // then assembly will split it into three sub commands [McsAssemblyComponentHandlers]
      // assembly will forward this sub commands to hcd in following sequence
      // 1. longSetup which takes 5 seconds to finish
      // 2. shortSetup which takes 1 second to finish
      // 3. mediumSetup which takes 3 seconds to finish
      // Test 1 submits the long command and gets the initial response that should be started because
      // it is a long-running command.  Then it monitors the various events that are generated to make sure
      // the test is running properly.  A queryFinal is then used to wait for the assembly command to complete
      // after all the subcommands to the HCDs have completed
      //#subscribe-for-result
      val test1InitialFuture   = assemblyCommandService.submit(assemblyLongSetup)
      val test1InitialResponse = Await.result(test1InitialFuture, 2.seconds)
      test1InitialResponse shouldBe a[Started]
      val test1RunId = test1InitialResponse.runId

      //#subscribe-for-result
      // verify that commands gets completed in following sequence
      // ShortSetup => MediumSetup => LongSetup
      probe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
      probe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
      probe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
      // This is the final command completing
      probe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted))))

      val test1Final = Await.result(assemblyCommandService.queryFinal(test1RunId), 15.seconds)
      test1Final shouldBe a[Completed]
      test1Final.runId shouldBe test1RunId
      // End of Test 1

      //#submit
      // Test 2 shows that submitAndWait can send the same command and wait for final completion
      val test2Response = assemblyCommandService.submitAndWait(assemblyLongSetup)
      //#submit

      val test2Final = Await.result(test2Response, 20.seconds)
      test2Final shouldBe a[Completed]
      // End of Test 2

      // Test 3 starts the long running command and uses query to examine the status of the command
      // prior to joining for completion with queryFinal allowing some work
      //#query-response
      val test3InitialResponse = Await.result(assemblyCommandService.submit(assemblyLongSetup), 5.seconds)
      val test3RunId           = test3InitialResponse.runId

      //do some work before querying for the result of above command as needed
      //Note at this point, the above submit would return quickly with the Started status so this is somewhat
      // redundant. This would allow you to see if it had completed since the first response
      val test3QueryResponse: Future[SubmitResponse] = assemblyCommandService.query(test3RunId)

      // Command is still just started
      //#query-response
      test3QueryResponse.map(_ shouldBe Started(test3RunId))

      // Use the initial future to determine the when completed
      val test3Final = Await.result(assemblyCommandService.queryFinal(test3RunId), 10.seconds)
      test3Final shouldBe a[Completed]
      test3Final.runId shouldEqual test3RunId

      enterBarrier("long-commands")

      // Used for invalid command tests
      val assemblyInvalidSetup = Setup(prefix, invalidCmd, Some(obsId))

      // First test sends two commands that complete immediately successfully
      //#submitAll
      val assemblyInitSetup = Setup(prefix, initCmd, Some(obsId))
      val assemblyMoveSetup = Setup(prefix, moveCmd, Some(obsId))

      val multiResponse1: Future[List[SubmitResponse]] =
        assemblyCommandService.submitAllAndWait(List(assemblyInitSetup, assemblyMoveSetup))
      //#submitAll

      whenReady(multiResponse1, PatienceConfiguration.Timeout(5.seconds)) { result =>
        result.length shouldBe 2
        result.head shouldBe a[Completed]
        result(1) shouldBe a[Completed]
      }

      // Second test sends three commands with last invalid
      val multiResponse2 =
        assemblyCommandService.submitAllAndWait(List(assemblyInitSetup, assemblyMoveSetup, assemblyInvalidSetup))
      whenReady(multiResponse2, PatienceConfiguration.Timeout(5.seconds)) { result =>
        result.length shouldBe 3
        result(0) shouldBe a[Completed]
        result(1) shouldBe a[Completed]
        result(2) shouldBe a[Invalid]
        result(2).asInstanceOf[Invalid].issue shouldBe OtherIssue("Invalid")
      }

      // Second test sends three commands with second invalid so last one is unexecuted
      val multiResponse3 =
        assemblyCommandService.submitAllAndWait(List(assemblyInitSetup, assemblyInvalidSetup, assemblyMoveSetup))
      whenReady(multiResponse3, PatienceConfiguration.Timeout(5.seconds)) { result =>
        result.length shouldBe 2
        result(0) shouldBe a[Completed]
        result(1) shouldBe a[Invalid]
        result(1).asInstanceOf[Invalid].issue shouldBe OtherIssue("Invalid")
      }

      // Last test does an init of assembly and then sends the long command
      val multiResponse4 = assemblyCommandService.submitAllAndWait(List(assemblyInitSetup, assemblyLongSetup))
      whenReady(multiResponse4, PatienceConfiguration.Timeout(10.seconds)) { result =>
        result.length shouldBe 2
        result(0) shouldBe a[Completed] //(assemblyInitSetup.runId)
        result(1) shouldBe a[Completed] // (assemblyLongSetup.runId)
      }

      enterBarrier("multiple-components-submit-multiple-commands")

      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }

    runOn(member1) {
      // spawn single assembly running in Standalone mode in jvm-2
      val wiring       = FrameworkWiring.make(typedSystem, locationService, mock[RedisClient])
      val assemblyConf = ConfigFactory.load("command/iris_assembly.conf")
      Await.result(Standalone.spawn(assemblyConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")
      enterBarrier("multiple-components-submit-multiple-commands")
      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }

    runOn(member2) {
      // spawn single hcd running in Standalone mode in jvm-3
      val wiring  = FrameworkWiring.make(typedSystem, locationService, mock[RedisClient])
      val hcdConf = ConfigFactory.load("command/iris_hcd.conf")
      Await.result(Standalone.spawn(hcdConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")
      enterBarrier("multiple-components-submit-multiple-commands")
      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }
    enterBarrier("end")
  }
}
