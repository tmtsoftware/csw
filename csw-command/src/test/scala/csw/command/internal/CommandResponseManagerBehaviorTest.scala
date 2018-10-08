package csw.command.internal

import akka.actor
import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.{ActorSystem, typed}
import csw.command.messages.CommandResponseManagerMessage._
import csw.command.messages.CommandResponseManagerMessage
import csw.command.models.{CommandCorrelation, CommandResponseManagerState}
import csw.params.commands.CommandResponse._
import csw.params.core.models.Id
import csw.logging.scaladsl.{Logger, LoggerFactory}
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

  def createBehaviorTestKit(): BehaviorTestKit[CommandResponseManagerMessage] = BehaviorTestKit(
    Behaviors.setup[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, getMockedLogger))
  )

  test("should be able to add command entry in Command Response Manager") {
    val behaviorTestKit         = createBehaviorTestKit()
    val commandResponseProbe    = TestProbe[QueryResponse]
    val commandCorrelationProbe = TestProbe[CommandCorrelation]
    val runId                   = Id()
    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))

    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Started(runId))

    behaviorTestKit.run(GetCommandCorrelation(commandCorrelationProbe.ref))
    commandCorrelationProbe.expectMessage(CommandCorrelation(Map.empty[Id, Set[Id]], Map.empty[Id, Id]))
  }


  test("should be able to add correlation between parent and child via AddSubCommand") {
    val behaviorTestKit         = createBehaviorTestKit()
    val commandCorrelationProbe = TestProbe[CommandCorrelation]
    val parentId                = Id()
    val childId                 = Id()

    behaviorTestKit.run(AddOrUpdateCommand(parentId, Started(parentId)))
    behaviorTestKit.run(AddSubCommand(parentId, childId))

    behaviorTestKit.run(GetCommandCorrelation(commandCorrelationProbe.ref))
    commandCorrelationProbe.expectMessage(CommandCorrelation(Map(parentId → Set(childId)), Map(childId → parentId)))
  }

  test("should be able to add subscriber and publish current state to newly added subscriber") {
    val behaviorTestKit                  = createBehaviorTestKit()
    val commandResponseProbe             = TestProbe[SubmitResponse]
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

  test("should get one update for long running command that returns Started") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[SubmitResponse]

    val runId = Id()

    // This simulates ComponentBehavior adding the command with Started - does not cause update to subscriber
    // Subscriber cannot subscribe before this happens, will not find command
    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    // Simulate doSubmit returning Started for a long-running command
    //behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    // Subscribe succeeds no after initial Started
    behaviorTestKit.run(Subscribe(runId, commandResponseProbe.ref))
    // Simulate doSubmit returning Started for a long-running command
    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    // Started is received to subscriber now
    commandResponseProbe.expectMessage(Started(runId))
  }

  test("should not be able to set value to Intermediate after Final in Command Response Manager") {
    val behaviorTestKit         = createBehaviorTestKit()
    val commandResponseProbe    = TestProbe[QueryResponse]
    val commandCorrelationProbe = TestProbe[CommandCorrelation]
    val runId                   = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Started(runId))

    behaviorTestKit.run(AddOrUpdateCommand(runId, Completed(runId)))
    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Completed(runId))

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Completed(runId))
  }

  test("should be able to remove subscriber") {
    val behaviorTestKit                  = createBehaviorTestKit()
    val commandResponseProbe             = TestProbe[SubmitResponse]
    val commandResponseManagerStateProbe = TestProbe[CommandResponseManagerState]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))

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
    val commandResponseProbe = TestProbe[QueryResponse]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))

    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Started(runId))
  }

  test("should be able to update and publish command status to all subscribers") {
    val behaviorTestKit                  = createBehaviorTestKit()
    val commandResponseProbe1            = TestProbe[SubmitResponse]
    val commandResponseProbe2            = TestProbe[SubmitResponse]
    val commandResponseManagerStateProbe = TestProbe[CommandResponseManagerState]

    val runId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
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
    val commandResponseProbe = TestProbe[SubmitResponse]

    val runId        = Id()
    val subCommandId = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    behaviorTestKit.run(Subscribe(runId, commandResponseProbe.ref))

    behaviorTestKit.run(AddSubCommand(runId, subCommandId))

    behaviorTestKit.run(UpdateSubCommand(subCommandId, Completed(subCommandId)))

    // Update of a sub command status(above) should update the status of parent command
    commandResponseProbe.expectMessage(Completed(runId))
  }

  // DEOPSCSW-208: Report failure on Configuration Completion command
  test("should be able to update command status with the status of subcommand if one of the subcommand fails") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[SubmitResponse]
    val runId                = Id()
    val subCommandId1        = Id()
    val subCommandId2        = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    behaviorTestKit.run(Subscribe(runId, commandResponseProbe.ref))

    behaviorTestKit.run(AddSubCommand(runId, subCommandId1))
    behaviorTestKit.run(AddSubCommand(runId, subCommandId2))

    behaviorTestKit.run(UpdateSubCommand(subCommandId1, Error(subCommandId1, "Sub command 1 failed")))
    behaviorTestKit.run(UpdateSubCommand(subCommandId2, Completed(subCommandId2)))

    // Update of a failed sub command status(above) should update the status of parent command as failed irrespective
    // of the result of other sub command
    commandResponseProbe.expectMessage(Error(runId, "Sub command 1 failed"))
  }

  // DEOPSCSW-207: Report on Configuration Command Completion
  test("should be able to update successful command status when all the subcommand completes with success") {
    val behaviorTestKit      = createBehaviorTestKit()
    val commandResponseProbe = TestProbe[QueryResponse]
    val runId                = Id()
    val subCommandId1        = Id()
    val subCommandId2        = Id()

    behaviorTestKit.run(AddOrUpdateCommand(runId, Started(runId)))
    behaviorTestKit.run(Subscribe(runId, commandResponseProbe.ref))

    behaviorTestKit.run(AddSubCommand(runId, subCommandId1))
    behaviorTestKit.run(AddSubCommand(runId, subCommandId2))

    // Update status of sub command 1 as completed
    behaviorTestKit.run(UpdateSubCommand(subCommandId1, Completed(subCommandId1)))

    // Status update of sub command 1 does not make the parent command complete
    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Started(runId))

    // Update status of sub command 2 with some intermediate status
    behaviorTestKit.run(UpdateSubCommand(subCommandId2, Started(subCommandId2)))

    // Status update of sub command 2 with intermediate does not make the parent command complete
    behaviorTestKit.run(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMessage(Started(runId))

    // Update status of sub command 2 as completed
    behaviorTestKit.run(UpdateSubCommand(subCommandId2, Completed(subCommandId2)))

    // Update of final sub command as Completed where other sub commands have completed earlier
    // should update the status of parent command as Completed
    commandResponseProbe.expectMessage(Completed(runId))
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
