package csw.services.ccs.internal

import akka.actor
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.{typed, ActorSystem}
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage._
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Error}
import csw.messages.ccs.commands.{CommandCorrelation, CommandResponse, CommandResponseManagerState}
import csw.messages.params.models.Id
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-207: Report on Configuration Command Completion
// DEOPSCSW-208: Report failure on Configuration Completion command
class CommandResponseManagerBehaviorTest extends FunSuite with Matchers with MockitoSugar {

  private val actorSystem                        = ActorSystem("test-command-status-service-system")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  def createBehaviorTestKit() = BehaviorTestKit(
    Behaviors.mutable[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, getMockedLogger))
  )

  test("should be able to add command entry in Command Response Manager") {
    val behaviorTestKit         = createBehaviorTestKit()
    val commandResponseProbe    = TestProbe[CommandResponse]
    val commandCorrelationProbe = TestProbe[CommandCorrelation]
    val runId                   = Id()
    behaviorTestKit.run(AddOrUpdateCommand(runId, Accepted(runId)))

    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Accepted(runId))

    behaviorTestKit.run(GetCommandCorrelation(commandCorrelationProbe.ref))
    commandCorrelationProbe.expectMessage(CommandCorrelation(Map.empty[Id, Set[Id]], Map.empty[Id, Id]))
  }

  test("should be able to add correlation between parent and child via AddSubCommand") {
    val behaviorTestKit         = createBehaviorTestKit()
    val commandCorrelationProbe = TestProbe[CommandCorrelation]
    val parentId                = Id()
    val childId                 = Id()

    behaviorTestKit.run(AddOrUpdateCommand(parentId, Accepted(parentId)))
    behaviorTestKit.run(AddSubCommand(parentId, childId))

    behaviorTestKit.run(GetCommandCorrelation(commandCorrelationProbe.ref))
    commandCorrelationProbe.expectMessage(CommandCorrelation(Map(parentId → Set(childId)), Map(childId → parentId)))
  }

  test("should be able to add subscriber and publish current state to newly added subscriber") {
    val behaviorTestKit                  = createBehaviorTestKit()
    val commandResponseProbe             = TestProbe[CommandResponse]
    val commandResponseManagerStateProbe = TestProbe[CommandResponseManagerState]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Completed(runId)))

    behaviorTestKit.run(GetCommandResponseManagerState(commandResponseManagerStateProbe.ref))
    val commandResponseManagerState = commandResponseManagerStateProbe.expectMessageType[CommandResponseManagerState]
    commandResponseManagerState.cmdToCmdStatus(runId).subscribers shouldBe empty

    behaviorTestKit.run(Subscribe(runId, commandResponseProbe.ref))
    behaviorTestKit.run(GetCommandResponseManagerState(commandResponseManagerStateProbe.ref))
    val commandResponseManagerState2 = commandResponseManagerStateProbe.expectMessageType[CommandResponseManagerState]
    commandResponseManagerState2.cmdToCmdStatus(runId).subscribers shouldBe Set(commandResponseProbe.ref)

    commandResponseProbe.expectMessage(Completed(runId))
  }

  test("should be able to remove subscriber") {
    val behaviorTestKit                  = createBehaviorTestKit()
    val commandResponseProbe             = TestProbe[CommandResponse]
    val commandResponseManagerStateProbe = TestProbe[CommandResponseManagerState]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Accepted(runId)))

    behaviorTestKit.run(Subscribe(runId, commandResponseProbe.ref))

    behaviorTestKit.run(GetCommandResponseManagerState(commandResponseManagerStateProbe.ref))
    val commandResponseManagerState = commandResponseManagerStateProbe.expectMessageType[CommandResponseManagerState]
    commandResponseManagerState.cmdToCmdStatus(runId).subscribers shouldBe Set(commandResponseProbe.ref)

    behaviorTestKit.run(Unsubscribe(runId, commandResponseProbe.ref))

    behaviorTestKit.run(GetCommandResponseManagerState(commandResponseManagerStateProbe.ref))
    val commandResponseManagerState2 = commandResponseManagerStateProbe.expectMessageType[CommandResponseManagerState]
    commandResponseManagerState2.cmdToCmdStatus(runId).subscribers shouldBe Set()
  }

  test("should be able to get current status of command on Query message") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[CommandResponse]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Accepted(runId)))

    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Accepted(runId))
  }

  test("should be able to update and publish command status to all subscribers") {
    val behaviorTestKit                  = createBehaviorTestKit()
    val commandResponseProbe1            = TestProbe[CommandResponse]
    val commandResponseProbe2            = TestProbe[CommandResponse]
    val commandResponseManagerStateProbe = TestProbe[CommandResponseManagerState]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Accepted(runId)))
    behaviorTestKit.run(Subscribe(runId, commandResponseProbe1.ref))
    behaviorTestKit.run(Subscribe(runId, commandResponseProbe2.ref))

    behaviorTestKit.run(AddOrUpdateCommand(runId, Completed(runId)))

    behaviorTestKit.run(GetCommandResponseManagerState(commandResponseManagerStateProbe.ref))
    val commandResponseManagerState = commandResponseManagerStateProbe.expectMessageType[CommandResponseManagerState]
    commandResponseManagerState.cmdToCmdStatus(runId).commandStatus.currentCmdStatus shouldBe Completed(runId)

    commandResponseProbe1.expectMessage(Completed(runId))
    commandResponseProbe2.expectMessage(Completed(runId))
  }

  test("should be able to infer command status when status of sub command is updated") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[CommandResponse]

    val commandId    = Id()
    val subCommandId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(commandId, Accepted(commandId)))
    behaviorTestKit.run(Subscribe(commandId, commandResponseProbe.ref))

    behaviorTestKit.run(AddSubCommand(commandId, subCommandId))

    behaviorTestKit.run(UpdateSubCommand(subCommandId, Completed(subCommandId)))

    // Update of a sub command status(above) should update the status of parent command
    commandResponseProbe.expectMessage(Completed(commandId))
  }

//  // DEOPSCSW-208: Report failure on Configuration Completion command
  test("should be able to update command status with the status of subcommand if one of the subcommand fails") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = Id()
    val subCommandId1        = Id()
    val subCommandId2        = Id()

    behaviorTestKit.run(AddOrUpdateCommand(commandId, Accepted(commandId)))
    behaviorTestKit.run(Subscribe(commandId, commandResponseProbe.ref))

    behaviorTestKit.run(AddSubCommand(commandId, subCommandId1))
    behaviorTestKit.run(AddSubCommand(commandId, subCommandId2))

    behaviorTestKit.run(UpdateSubCommand(subCommandId1, Error(subCommandId1, "Sub command 1 failed")))
    behaviorTestKit.run(UpdateSubCommand(subCommandId2, Completed(subCommandId2)))

    // Update of a failed sub command status(above) should update the status of parent command as failed irrespective
    // of the result of other sub command
    commandResponseProbe.expectMessage(Error(commandId, "Sub command 1 failed"))
  }

//  // DEOPSCSW-207: Report on Configuration Command Completion
  test("should be able to update successful command status when all the subcommand completes with success") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = Id()
    val subCommandId1        = Id()
    val subCommandId2        = Id()

    behaviorTestKit.run(AddOrUpdateCommand(commandId, Accepted(commandId)))
    behaviorTestKit.run(Subscribe(commandId, commandResponseProbe.ref))

    behaviorTestKit.run(AddSubCommand(commandId, subCommandId1))
    behaviorTestKit.run(AddSubCommand(commandId, subCommandId2))

    // Update status of sub command 1 as completed
    behaviorTestKit.run(UpdateSubCommand(subCommandId1, Completed(subCommandId1)))

    // Status update of sub command 1 does not make the parent command complete
    behaviorTestKit.run(Query(commandId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Accepted(commandId))

    // Update status of sub command 2 with some intermediate status
    behaviorTestKit.run(UpdateSubCommand(subCommandId2, Accepted(subCommandId2)))

    // Status update of sub command 2  with intermediate does not make the parent command complete
    behaviorTestKit.run(Query(commandId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Accepted(commandId))

    // Update status of sub command 2 as completed
    behaviorTestKit.run(UpdateSubCommand(subCommandId2, Completed(subCommandId2)))

    // Update of final sub command as Completed where other sub commands have completed earlier
    // should update the status of parent command as Completed
    commandResponseProbe.expectMessage(Completed(commandId))
  }

  private def getMockedLogger: LoggerFactory = {
    val loggerFactory: LoggerFactory = mock[LoggerFactory]
    val logger: Logger               = mock[Logger]

    when(loggerFactory.getLogger).thenReturn(logger)
    when(loggerFactory.getLogger(any[actor.ActorContext])).thenReturn(logger)
    when(loggerFactory.getLogger(any[ActorContext[_]])).thenReturn(logger)

    loggerFactory
  }
}
