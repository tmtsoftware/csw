package example.command

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.api.{DemandMatcher, StateMatcher}
import csw.command.client.CommandServiceFactory
import csw.location.api.models
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.*
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse.*
import csw.params.core.generics.Parameter
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.prefix.models.Subsystem.CSW
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.async.Async.*
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}

class CommandExample()
    extends ScalaTestFrameworkTestKit(EventServer)
    with AnyFunSuiteLike
    with MockitoSugar
    with Matchers
    with Eventually {

  import org.tmt.csw.sample.ComponentStateForCommand.*

  private implicit val timeout: Timeout                                = 5.seconds
  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  private implicit val ec: ExecutionContext                            = typedSystem.executionContext
  implicit private val patience: PatienceConfig                        = PatienceConfig(5.seconds, 100.millis)
  val locationService: LocationService                                 = frameworkTestKit.locationService
  private val loggerFactory                                            = new LoggerFactory(Prefix(CSW, "command.example"))
  val log                                                              = loggerFactory.getLogger
  test(s"$prefix sender of command should receive appropriate responses") {
    implicit val timeout: Timeout     = 5.seconds
    implicit val ec: ExecutionContext = typedSystem.executionContext

    val obsId = Some(ObsId("2020A-001-123"))

    // spawn single assembly running in Standalone mode in jvm-2
    val sampleHcdConf = ConfigFactory.load("SampleHcdStandalone.conf")
    spawnStandalone(sampleHcdConf)
    val sampleHcdLocF =
      locationService.resolve(
        AkkaConnection(models.ComponentId(Prefix(Subsystem.CSW, "samplehcd"), ComponentType.HCD)),
        15.seconds
      )
    val sampleHcdLocation: AkkaLocation = Await.result(sampleHcdLocF, 10.seconds).get
    val assemblyConf                    = ConfigFactory.load("commanding_assembly.conf")
    spawnContainer(assemblyConf)
    // resolve assembly running in jvm-3 and send setup command expecting immediate command completion response
    // #resolve-hcd-and-create-commandservice
    val assemblyLocF =
      locationService.resolve(
        AkkaConnection(models.ComponentId(Prefix(Subsystem.WFOS, "Assembly"), ComponentType.Assembly)),
        15.seconds
      )
    val assemblyLocation: AkkaLocation = Await.result(assemblyLocF, 10.seconds).get
    val assemblyCmdService             = CommandServiceFactory.make(assemblyLocation)
    // #resolve-hcd-and-create-commandservice
    // resolve assembly running in jvm-3 and send setup command expecting immediate command completion response
    val hcdLocF =
      locationService.resolve(AkkaConnection(models.ComponentId(Prefix(Subsystem.WFOS, "HCD"), ComponentType.HCD)), 15.seconds)
    val hcdLocation: AkkaLocation = Await.result(hcdLocF, 15.seconds).get
    val hcdCmdService             = CommandServiceFactory.make(hcdLocation)

    // #invalidCmd
    val invalidSetup    = Setup(prefix, invalidCmd, obsId)
    val invalidCommandF = assemblyCmdService.submitAndWait(invalidSetup)
    val resultF: Future[Unit] = async {
      await(invalidCommandF) match {
        case Completed(_, _) =>
        // Do Completed thing
        case Invalid(_, _) =>
        // issue shouldBe a[Invalid]
        case other =>
          // Unexpected result
          log.error(s"Some other response: $other")
      }
    }
    // #invalidCmd
    Await.result(invalidCommandF, 5.seconds) shouldBe a[Invalid]

    // just to remove the warning of unused and the nullary method. Cannot add @nowarn as it is a snippet for docs.
    Await.result(resultF, 5.seconds)

    // short running command
    // #immediate-response
    val immediateSetup = Setup(prefix, immediateCmd, obsId)
    val immediateCommandF: Future[SubmitResponse] = async {
      await(assemblyCmdService.submitAndWait(immediateSetup)) match {
        case response: Completed =>
          // do something with completed result
          response
        case otherResponse =>
          // do something with other response which is not expected
          otherResponse
      }
    }
    // #immediate-response
    Await.result(immediateCommandF, timeout.duration) shouldBe a[Completed]

    // #longRunning
    val longRunningSetup = Setup(prefix, longRunningCmd, obsId)

    val longRunningResultF: Future[Option[Int]] = async {
      await(assemblyCmdService.submitAndWait(longRunningSetup)) match {
        case Completed(_, result) =>
          result.nonEmpty shouldBe true
          Some(result(encoder).head)

        case otherResponse =>
          // log a message?
          None
      }
    }
    // #longRunning
    val longRunningResult = Await.result(longRunningResultF, timeout.duration)
    longRunningResult shouldBe Some(20)

    // long running command which does not use matcher
    var longRunningRunId: Id = Id("blah") // Is updated below for use in later test

    // #queryLongRunning
    val longRunningQueryResultF: Future[Option[Int]] = async {
      // The following val is set so we can do query and work and complete later
      val longRunningF = assemblyCmdService.submit(longRunningSetup)
      // This is used in a later test
      longRunningRunId = await(longRunningF).runId

      await(assemblyCmdService.query(longRunningRunId)) match {
        case Started(runId) =>
          runId shouldEqual longRunningRunId
        // happy case - no action needed
        // Do some other work
        case a =>
        // log.error. This indicates that the command probably failed to start.
      }

      // Now wait for completion and result
      await(assemblyCmdService.queryFinal(longRunningRunId)) match {
        case Completed(_, result) =>
          Some(result(encoder).head)

        case otherResponse =>
          // log a message?
          None
      }
    }
    // #queryLongRunning
    Await.result(longRunningQueryResultF, timeout.duration) shouldBe Some(20)

    // This test shows DEOPSCSW-623 because submit is issued without future and queryFinal works
    // #queryFinal
    val queryFinalF: Future[Option[Int]] = async {
      // The following submit is made without saving the Future!
      val runId = await(assemblyCmdService.submit(longRunningSetup)).runId

      // Use queryFinal and runId to wait for completion and result
      await(assemblyCmdService.queryFinal(runId)) match {
        case Completed(_, result) =>
          Some(result(encoder).head)

        case otherResponse =>
          // log a message?
          None
      }
    }
    // #queryFinal
    Await.result(queryFinalF, timeout.duration) shouldBe Some(20)

    // #queryFinalWithSubmitAndWait
    val encoderValue: Future[Option[Int]] = async {
      // The following submit is made without saving the Future!
      val runId = await(assemblyCmdService.submitAndWait(longRunningSetup)).runId

      // Use queryFinal and runId to wait for completion and result
      await(assemblyCmdService.queryFinal(runId)) match {
        case Completed(_, result) =>
          Some(result(encoder).head)

        case otherResponse =>
          // log a message?
          None
      }
    }
    // #queryFinalWithSubmitAndWait
    Await.result(encoderValue, timeout.duration) shouldBe Some(20)

    // #oneway
    // `onewayCmd` is a sample to demonstrate oneway without any actions
    val onewaySetup = Setup(prefix, onewayCmd, obsId)
    // Don't care about the futures from async
    val oneWayF: Future[Unit] = async {
      await(assemblyCmdService.oneway(onewaySetup)) match {
        case invalid: Invalid =>
        // Log an error here
        case _ =>
        // Ignore anything other than invalid
      }
    }
    Await.ready(oneWayF, timeout.duration)
    // #oneway

    // #validate
    val validateCommandF: Future[Boolean] = async {
      await(assemblyCmdService.validate(immediateSetup)) match {
        case _: Accepted       => true
        case Invalid(_, issue) =>
          // do something with other response which is not expected
          log.error(s"Command failed to validate with issue: $issue")
          false
        case _: Locked => false
      }
    }
    // #validate
    Await.result(validateCommandF, timeout.duration) shouldBe true

    // test CommandNotAvailable after timeout of 1 seconds
    Await.result(assemblyCmdService.query(Id("blah")), 2.seconds) shouldEqual
    Invalid(Id("blah"), IdNotAvailableIssue(Id("blah").id))

    // #query
    // Check on a command that was completed in the past
    val queryValue = Await.result(assemblyCmdService.query(longRunningRunId), timeout.duration)
    // #query
    queryValue shouldBe a[Completed]

    val submitAllSetup1       = Setup(prefix, immediateCmd, obsId)
    val submitAllSetup2       = Setup(prefix, longRunningCmd, obsId)
    val submitAllinvalidSetup = Setup(prefix, invalidCmd, obsId)

    // #submitAll
    val submitAllF: Future[List[SubmitResponse]] = async {
      await(assemblyCmdService.submitAllAndWait(List(submitAllSetup1, submitAllSetup2, submitAllinvalidSetup)))
    }
    val submitAllResponse = Await.result(submitAllF, timeout.duration)
    // #submitAll
    submitAllResponse.length shouldBe 3
    submitAllResponse(0) shouldBe a[Completed]
    submitAllResponse(1) shouldBe a[Completed]
    submitAllResponse(2) shouldBe a[Invalid]

    // #submitAllInvalid
    val submitAllF2: Future[List[SubmitResponse]] = async {
      await(assemblyCmdService.submitAllAndWait(List(submitAllSetup1, submitAllinvalidSetup, submitAllSetup2)))
    }
    val submitAllResponse2 = Await.result(submitAllF2, timeout.duration)
    // #submitAllInvalid
    submitAllResponse2.length shouldBe 2
    submitAllResponse2(0) shouldBe a[Completed]
    submitAllResponse2(1) shouldBe a[Invalid]

    // #subscribeCurrentState
    // Subscriber code
    val expectedEncoderValue = 234
    val currStateSetup       = Setup(prefix, hcdCurrentStateCmd, obsId).add(encoder.set(expectedEncoderValue))
    // Setup a callback response to CurrentState
    var cstate: CurrentState = CurrentState(prefix, StateName("no cstate"), Set.empty)
    val subscription         = hcdCmdService.subscribeCurrentState(cs => cstate = cs)
    // Send a oneway to the HCD that will cause it to publish a CurrentState with the encoder value
    // in the command parameter "encoder". Callback will store value into cstate.
    hcdCmdService.oneway(currStateSetup)

    // Eventually current state callback will get invoked when hcd publish its state
    // Test to see if value was received
    eventually(cstate(encoder).head shouldBe expectedEncoderValue)

    // Unsubscribe to CurrentState
    subscription.cancel()
    // #subscribeCurrentState

    // long running command which uses matcher
    // #matcher
    val param: Parameter[Int] = encoder.set(100)
    val setupWithMatcher      = Setup(prefix, matcherCmd, obsId)

    // create a StateMatcher which specifies the desired algorithm and state to be matched.
    val demandMatcher: StateMatcher =
      DemandMatcher(DemandState(prefix, StateName("testStateName")).add(param), withUnits = false, timeout)

    // Submit command as a oneway and if the command is successfully validated,
    // check for matching of demand state against current state
    val matchResponseF: Future[MatchingResponse] = assemblyCmdService.onewayAndMatch(setupWithMatcher, demandMatcher)

    val commandResponse = Await.result(matchResponseF, timeout.duration)
    // #matcher
    commandResponse shouldBe a[Completed]

    // #onewayAndMatch
    val onewayMatchF: Future[SubmitResponse with MatchingResponse] = async {
      await(assemblyCmdService.onewayAndMatch(setupWithMatcher, demandMatcher)) match {
        case i: Invalid =>
          // Command was not accepted
          log.error(s"Oneway match was not accepted: ${i.issue}")
          i
        case c: Completed =>
          // Do some completed work
          c
        case e: Error =>
          // Match failed and timedout generating an error - log a message
          log.error(s"Oeway match produced an error: ${e.message}")
          e
        case l: Locked =>
          // Destination component was locked, log a message
          log.error(s"Destination component was locked")
          l
      }
    }
    // #onewayAndMatch
    Await.result(onewayMatchF, timeout.duration) shouldBe a[Completed]

  }

}
