package csw.command

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.messages.CommandMessage.Submit
import csw.messages.SupervisorLockMessage.Lock
import csw.messages.ccs.CommandIssue.ComponentLockedIssue
import csw.messages.ccs.commands.CommandResponse._
import csw.messages.ccs.commands.{CommandResponse, Observe, Setup}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.models.LockingResponse
import csw.messages.models.LockingResponse.LockAcquired
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.ObsId
import csw.messages.params.states.DemandState
import csw.services.ccs.common.ActorRefExts.RichComponentActor
import csw.services.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.services.ccs.internal.matchers.{DemandMatcher, Matcher}
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class CommandServiceTestMultiJvm1 extends CommandServiceTest(0)
class CommandServiceTestMultiJvm2 extends CommandServiceTest(0)
class CommandServiceTestMultiJvm3 extends CommandServiceTest(0)

// DEOPSCSW-201: Destination component to receive a submit command
// DEOPSCSW-202: Verification of submit commands
// DEOPSCSW-207: Report on Configuration Command Completion
// DEOPSCSW-208: Report failure on Configuration Completion command
// DEOPSCSW-222: Locking a component for a specific duration
// DEOPSCSW-313: Support short running actions by providing immediate response
class CommandServiceTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._
  import csw.common.components.command.ComponentStateForCommand._

  implicit val actorSystem: ActorSystem[_]  = system.toTyped
  implicit val mat: Materializer            = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout             = 5.seconds
  implicit val scheduler: Scheduler         = actorSystem.scheduler
  implicit val testkit: TestKitSettings     = TestKitSettings(actorSystem)

  test("sender of command should receive appropriate responses") {

    runOn(seed) {
      // cluster seed is running on jvm-1
      enterBarrier("spawned")

      // resolve assembly running in jvm-3 and send setup command expecting immediate command completion response
      val assemblyLocF = locationService.resolve(AkkaConnection(ComponentId("Assembly", ComponentType.Assembly)), 5.seconds)
      val assemblyRef  = Await.result(assemblyLocF, 10.seconds).map(_.componentRef()).get

      enterBarrier("short-long-commands")
      enterBarrier("assembly-locked")

      val obsId            = ObsId("Obs002")
      val cmdResponseProbe = TestProbe[CommandResponse]

      // try to send a command to assembly which is already locked
      val assemblyObserve = Observe(obsId, acceptedCmdPrefix)
      assemblyRef ! Submit(assemblyObserve, cmdResponseProbe.ref)
      val response = cmdResponseProbe.expectMsgType[NotAllowed]
      response.issue shouldBe an[ComponentLockedIssue]

      enterBarrier("command-when-locked")
    }

    runOn(member1) {
      val obsId            = ObsId("Obs001")
      val cmdResponseProbe = TestProbe[CommandResponse]

      // spawn single assembly running in Standalone mode in jvm-2
      val wiring        = FrameworkWiring.make(system, locationService)
      val sequencerConf = ConfigFactory.load("command/commanding_assembly.conf")
      Await.result(Standalone.spawn(sequencerConf, wiring), 5.seconds)
      enterBarrier("spawned")

      // resolve assembly running in jvm-3 and send setup command expecting immediate command completion response
      val assemblyLocF = locationService.resolve(AkkaConnection(ComponentId("Assembly", ComponentType.Assembly)), 5.seconds)
      val assemblyRef  = Await.result(assemblyLocF, 10.seconds).map(_.componentRef()).get

      // short running command
      val shortCommandResponse = Await.result(assemblyRef.submit(Setup(obsId, invalidCmdPrefix)), timeout.duration)
      shortCommandResponse shouldBe a[Invalid]

      // long running command which does not use matcher
      val setupWithoutMatcher = Setup(obsId, acceptWithNoMatcherCmdPrefix)
      val longCommandResponse = Await.result(
        assemblyRef.submit(setupWithoutMatcher).flatMap {
          case _: Accepted ⇒ assemblyRef.getCommandResponse(setupWithoutMatcher.runId)
          case x           ⇒ Future.successful(x)
        },
        timeout.duration
      )
      longCommandResponse shouldBe a[CompletedWithResult]
      longCommandResponse.runId shouldBe setupWithoutMatcher.runId

      // long running command which uses matcher
      val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(100)
      val demandMatcher         = DemandMatcher(DemandState(acceptWithMatcherCmdPrefix, Set(param)), withUnits = false, timeout)
      val setupWithMatcher      = Setup(obsId, acceptWithMatcherCmdPrefix)
      val matcher               = new Matcher(assemblyRef, demandMatcher)

      val matchedResponse = Await.result(
        assemblyRef.oneway(setupWithMatcher).flatMap {
          case _: Accepted ⇒
            matcher.response.map {
              case MatchCompleted  ⇒ Completed(setupWithMatcher.runId)
              case MatchFailed(ex) ⇒ Error(setupWithMatcher.runId, ex.getMessage)
            }
          case x: Invalid ⇒
            matcher.stop()
            Future.successful(x)
          case x ⇒ Future.successful(x)
        },
        timeout.duration
      )
      matchedResponse shouldBe Completed(setupWithMatcher.runId)
      enterBarrier("short-long-commands")

      // acquire lock on assembly
      val lockResponseProbe = TestProbe[LockingResponse]
      assemblyRef ! Lock(immediateCmdPrefix, lockResponseProbe.ref)
      lockResponseProbe.expectMsg(LockAcquired)
      enterBarrier("assembly-locked")

      // send command with lock token and expect command processing response
      val assemblySetup = Setup(obsId, immediateCmdPrefix)
      assemblyRef ! Submit(assemblySetup, cmdResponseProbe.ref)
      cmdResponseProbe.expectMsg(5.seconds, Completed(assemblySetup.runId))
      enterBarrier("command-when-locked")
    }

    runOn(member2) {
      // spawn container having assembly and hcd running in jvm-3
      val wiring        = FrameworkWiring.make(system, locationService)
      val containerConf = ConfigFactory.load("command/container.conf")
      Await.result(Container.spawn(containerConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("short-long-commands")
      enterBarrier("assembly-locked")
      enterBarrier("command-when-locked")
    }

    enterBarrier("end")
  }
}
