package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.ccs.StateMatcher
import csw.messages.PubSub.Publish
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages._
import csw.trombone.assembly.Matchers

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

trait AssemblyCommand {
  def startCommand(): Future[CommandExecutionResponse]
  def stopCommand(): Unit

  final def publishState(assemblyState: AssemblyState, stateActor: ActorRef[PubSub[AssemblyState]]): Unit =
    stateActor ! Publish(assemblyState)

  final def matchCompletion(
      ctx: ActorContext[_],
      stateMatcher: StateMatcher,
      currentStateSource: ActorRef[ComponentStateSubscription],
      timeout: Timeout = Timeout(5.seconds)
  )(
      partialFunction: PartialFunction[CommandExecutionResponse, CommandExecutionResponse]
  ): Future[CommandExecutionResponse] = {
    import ctx.executionContext
    Matchers.matchState(ctx, stateMatcher, currentStateSource, timeout).map(partialFunction)
  }

  final def responseCompletion[T](ctx: ActorContext[_], destination: ActorRef[T], command: T, timeout: Timeout)(
      partialFunction: PartialFunction[CommandExecutionResponse, CommandExecutionResponse]
  ): Future[CommandExecutionResponse] = {

    import ctx.executionContext

    (destination ? { x: ActorRef[CommandExecutionResponse] ⇒
      command
    })(timeout, ctx.system.scheduler).map(partialFunction)
  }
}
