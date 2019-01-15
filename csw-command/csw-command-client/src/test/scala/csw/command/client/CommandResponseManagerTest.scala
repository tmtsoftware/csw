package csw.command.client
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem}
import akka.util.Timeout
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.messages.CommandResponseManagerMessage._
import csw.params.commands.CommandResponse.Completed
import csw.params.core.models.Id
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

class CommandResponseManagerTest extends FunSuite with Matchers with MockitoSugar {
  implicit val actorSystem: ActorSystem          = ActorSystem("test-command-response-manager")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped

  test("should delegate to addOrUpdateCommand") {
    val commandResponseManagerProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseManager      = new CommandResponseManager(commandResponseManagerProbe.ref)
    val commandResponse             = Completed(Id("test-id"))

    commandResponseManager.addOrUpdateCommand(commandResponse)

    commandResponseManagerProbe.expectMessage(AddOrUpdateCommand(commandResponse))
  }

  test("should delegate to addSubCommand") {
    val commandResponseManagerProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseManager      = new CommandResponseManager(commandResponseManagerProbe.ref)
    val parentRunId                 = Id("0000")
    val childRunId                  = Id("1111")

    commandResponseManager.addSubCommand(parentRunId, childRunId)

    commandResponseManagerProbe.expectMessage(AddSubCommand(parentRunId, childRunId))
  }

  test("should delegate to updateSubCommand") {
    val commandResponseManagerProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseManager      = new CommandResponseManager(commandResponseManagerProbe.ref)
    val commandResponse             = Completed(Id("test-id"))

    commandResponseManager.updateSubCommand(commandResponse)

    commandResponseManagerProbe.expectMessage(UpdateSubCommand(commandResponse))
  }

  test("should delegate to query") {
    val commandResponseManagerProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseManager      = new CommandResponseManager(commandResponseManagerProbe.ref)
    implicit val timeOut: Timeout   = Timeout(10.seconds)
    val runId                       = Id("1111")

    commandResponseManager.query(runId)

    commandResponseManagerProbe.expectMessageType[Query]
  }

  test("queryFinal should delegate to Subscribe") {
    val commandResponseManagerProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseManager      = new CommandResponseManager(commandResponseManagerProbe.ref)
    implicit val timeOut: Timeout   = Timeout(10.seconds)
    val runId                       = Id("1111")

    commandResponseManager.queryFinal(runId)

    commandResponseManagerProbe.expectMessageType[Subscribe]
  }
}
