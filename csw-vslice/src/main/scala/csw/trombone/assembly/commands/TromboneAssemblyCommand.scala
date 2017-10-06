package csw.trombone.assembly.commands

import csw.messages.CommandExecutionResponse

import scala.concurrent.Future

trait TromboneAssemblyCommand {
  def startCommand(): Future[CommandExecutionResponse]
  def stopCurrentCommand(): Unit
}
