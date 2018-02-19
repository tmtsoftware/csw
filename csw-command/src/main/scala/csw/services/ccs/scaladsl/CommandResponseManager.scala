package csw.services.ccs.scaladsl

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage.{AddOrUpdateCommand, AddSubCommand, Query, UpdateSubCommand}
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.Id

import scala.concurrent.Future

class CommandResponseManager(val commandResponseManagerActor: ActorRef[CommandResponseManagerMessage])(
    implicit scheduler: Scheduler
) {

  def addOrUpdateCommand(commandId: Id, cmdStatus: CommandResponse): Unit =
    commandResponseManagerActor ! AddOrUpdateCommand(commandId, cmdStatus)

  def addSubCommand(parentRunId: Id, childRunId: Id): Unit =
    commandResponseManagerActor ! AddSubCommand(parentRunId, childRunId)

  def updateSubCommand(subCommandId: Id, cmdStatus: CommandResponse): Unit =
    commandResponseManagerActor ! UpdateSubCommand(subCommandId, cmdStatus)

  def query(runId: Id)(implicit timeout: Timeout): Future[CommandResponse] =
    commandResponseManagerActor ? (Query(runId, _))

}
