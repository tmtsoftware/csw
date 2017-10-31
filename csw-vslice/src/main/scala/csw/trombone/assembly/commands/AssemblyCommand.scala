package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.ccs.StateMatcher
import csw.messages.PubSub.Publish
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages._
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, MatcherResponse, Matchers}

import scala.concurrent.Future

abstract class AssemblyCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    startState: AssemblyState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) {
  import ctx.executionContext

  def startCommand(): Future[CommandExecutionResponse]
  def stopCommand(): Unit

  final def publishState(assemblyState: AssemblyState): Unit =
    stateActor ! Publish(assemblyState)

  final def matchCompletion(
      stateMatcher: StateMatcher,
      currentStateSource: ActorRef[ComponentStateSubscription],
      timeout: Timeout
  )(partialFunction: PartialFunction[MatcherResponse, CommandExecutionResponse]): Future[CommandExecutionResponse] =
    Matchers.matchState(ctx, stateMatcher, currentStateSource, timeout).map(partialFunction)

  final def responseCompletion[T](destination: ActorRef[T], command: T, timeout: Timeout)(
      partialFunction: PartialFunction[CommandExecutionResponse, CommandExecutionResponse]
  ): Future[CommandExecutionResponse] =
    (destination ? execute(command))(timeout, ctx.system.scheduler).map(partialFunction)

  private def execute[T](x: T)(replyTo: ActorRef[CommandExecutionResponse]): T = x
}
