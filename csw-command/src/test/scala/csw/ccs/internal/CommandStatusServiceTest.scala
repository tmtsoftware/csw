package csw.ccs.internal

import akka.actor.ActorSystem
import akka.typed
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import csw.messages.CommandStatusMessages
import csw.messages.CommandStatusMessages._
import csw.messages.ccs.commands.CommandExecutionResponse.{Completed, InProgress}
import csw.messages.ccs.commands.CommandResponse
import csw.messages.ccs.commands.CommandValidationResponse.Accepted
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.{ComponentLogger, Logger}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class CommandStatusServiceTest extends FunSuite with Matchers {

  trait MutableActorMock[T] { this: ComponentLogger.MutableActor[T] ⇒
    override protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  private val actorSystem                        = ActorSystem("test-command-status-service-system")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val ctx: StubbedActorContext[CommandStatusMessages] =
    new StubbedActorContext[CommandStatusMessages]("ctx-command-status-service", 100, typedSystem)

  def createCommandStatusService(): CommandStatusService =
    new CommandStatusService(ctx, "test-component") with MutableActorMock[CommandStatusMessages]

  test("should add command") {
    val commandStatusService = createCommandStatusService()

    val runId = RunId()
    commandStatusService.onMessage(AddCommand(runId, Accepted(runId)))

    commandStatusService.commandStatus.get(runId) shouldBe Accepted(runId)
  }

  test("should sub command related to an existing command") {
    val commandStatusService = createCommandStatusService()
    val parentId             = RunId()
    val childId              = RunId()

    commandStatusService.onMessage(AddSubCommand(parentId, childId))

    commandStatusService.commandManagerState.childToParent(childId) shouldBe parentId
    commandStatusService.commandManagerState.parentToChildren(parentId) shouldBe Set(childId)
  }

  test("should add subscriber and publish current state to newly added subscriber") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val runId                = RunId()

    commandStatusService.onMessage(AddCommand(runId, Completed(runId)))
    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe.ref))

    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers should contain(
      commandResponseProbe.ref
    )

    commandResponseProbe.expectMsg(Completed(runId))
  }

  test("should remove subscriber") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val runId                = RunId()

    commandStatusService.onMessage(AddCommand(runId, Accepted(runId)))
    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe.ref))

    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers should contain(commandResponseProbe.ref)

    commandStatusService.onMessage(UnSubscribe(runId, commandResponseProbe.ref))

    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers should not contain commandResponseProbe.ref
  }

  test("should provide current status on querying command") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val runId                = RunId()

    commandStatusService.onMessage(AddCommand(runId, Accepted(runId)))
    commandStatusService.onMessage(Query(runId, commandResponseProbe.ref))

    commandResponseProbe.expectMsg(Accepted(runId))
  }

  test("should update command status and publish update to all subscribers") {
    val commandStatusService  = createCommandStatusService()
    val commandResponseProbe1 = TestProbe[CommandResponse]
    val commandResponseProbe2 = TestProbe[CommandResponse]
    val runId                 = RunId()

    commandStatusService.onMessage(AddCommand(runId, Accepted(runId)))
    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe1.ref))
    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe2.ref))

    commandStatusService.onMessage(UpdateCommand(InProgress(runId, "40% completed")))

    commandResponseProbe1.expectMsg(InProgress(runId, "40% completed"))
    commandResponseProbe2.expectMsg(InProgress(runId, "40% completed"))
  }
}
