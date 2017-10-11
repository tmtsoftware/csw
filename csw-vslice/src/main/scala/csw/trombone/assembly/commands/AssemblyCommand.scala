package csw.trombone.assembly.commands

import csw.ccs.DemandMatcher
import csw.messages.{CommandExecutionResponse, NoLongerValid}

import scala.concurrent.Future

trait AssemblyCommand {
  def startCommand(): Future[CommandExecutionResponse]
  def isAssemblyStateValid: Boolean
  def sendInvalidCommandResponse: Future[NoLongerValid]
  def publishInitialState(): Unit
  def matchState(stateMatcher: DemandMatcher): Future[CommandExecutionResponse]
  def stopCurrentCommand(): Unit
  def sendState(setState: AssemblyState): Unit
}
