package csw.ccs.internal

import akka.actor.ActorSystem
import akka.typed
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage._
import csw.messages.ccs.commands.CommandExecutionResponse.{Completed, Error, InProgress}
import csw.messages.ccs.commands.CommandResponse
import csw.messages.ccs.commands.CommandValidationResponse.Accepted
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.{ComponentLogger, Logger}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class CommandResponseManagerBehaviorTest extends FunSuite with Matchers {

  trait MutableActorMock[T] { this: ComponentLogger.MutableActor[T] ⇒
    override protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  private val actorSystem                        = ActorSystem("test-command-status-service-system")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val ctx: StubbedActorContext[CommandResponseManagerMessage] =
    new StubbedActorContext[CommandResponseManagerMessage]("ctx-command-status-service", 100, typedSystem)

  def createCommandStatusService(): CommandResponseManagerBehavior =
    new CommandResponseManagerBehavior(ctx, "test-component") with MutableActorMock[CommandResponseManagerMessage]

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

    commandStatusService.commandCoRelation.childToParent(childId) shouldBe parentId
    commandStatusService.commandCoRelation.parentToChildren(parentId) shouldBe Set(childId)
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

  test("should infer command status when status of sub command is updated") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = RunId()
    val subCommandId         = RunId()

    commandStatusService.onMessage(AddCommand(commandId, Accepted(commandId)))
    commandStatusService.onMessage(Subscribe(commandId, commandResponseProbe.ref))

    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId))

    commandStatusService.onMessage(UpdateSubCommand(Completed(subCommandId)))

    // Update of a sub command status(above) should update the status of parent command
    commandResponseProbe.expectMsg(Completed(commandId))
  }

  test("should update command status with the status of subcommand if one of the subcommand fails") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = RunId()
    val subCommandId1        = RunId()
    val subCommandId2        = RunId()

    commandStatusService.onMessage(AddCommand(commandId, Accepted(commandId)))
    commandStatusService.onMessage(Subscribe(commandId, commandResponseProbe.ref))

    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId1))
    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId2))

    commandStatusService.onMessage(UpdateSubCommand(Error(subCommandId1, "Sub command 1 failed")))
    commandStatusService.onMessage(UpdateSubCommand(Completed(subCommandId2)))

    // Update of a failed sub command status(above) should update the status of parent command as failed irrespective
    // of the result of other sub command
    commandResponseProbe.expectMsg(Error(commandId, "Sub command 1 failed"))
  }

  test("should update successful command status when all the subcommand completes with success") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = RunId()
    val subCommandId1        = RunId()
    val subCommandId2        = RunId()

    commandStatusService.onMessage(AddCommand(commandId, Accepted(commandId)))
    commandStatusService.onMessage(Subscribe(commandId, commandResponseProbe.ref))

    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId1))
    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId2))

    // Update status of sub command 1 as completed
    commandStatusService.onMessage(UpdateSubCommand(Completed(subCommandId1)))

    // Status update of sub command 1 does not make the parent command complete
    commandStatusService.onMessage(Query(commandId, commandResponseProbe.ref))
    commandResponseProbe.expectMsg(Accepted(commandId))

    // Update status of sub command 2 with some intermediate status
    commandStatusService.onMessage(UpdateSubCommand(InProgress(subCommandId2)))

    // Status update of sub command 2  with intermediate does not make the parent command complete
    commandStatusService.onMessage(Query(commandId, commandResponseProbe.ref))
    commandResponseProbe.expectMsg(Accepted(commandId))

    // Update status of sub command 2 as completed
    commandStatusService.onMessage(UpdateSubCommand(Completed(subCommandId2)))

    // Update of final sub command as Completed where other sub commands have completed earlier
    // should update the status of parent command as Completed
    commandResponseProbe.expectMsg(Completed(commandId))
  }
}
