package csw.services.ccs.common

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.params.models.RunId
import csw.messages.{CommandResponseManagerMessage, SupervisorExternalMessage}

import scala.concurrent.Future

object ActorRefExts {
  implicit class RichActor[A](val ref: ActorRef[A]) extends AnyVal {
    def ask[B](f: ActorRef[B] ⇒ A)(implicit timeout: Timeout, scheduler: Scheduler): Future[B] = ref ? f
  }

  implicit class RichComponentActor(val componentActor: ActorRef[SupervisorExternalMessage]) extends AnyVal {
    def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Submit(controlCommand, _))

    def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Oneway(controlCommand, _))

    def getCommandResponse(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  }
}

/*class FunctionBuilder[I, R, S](hof: (I ⇒ R) ⇒ S) {
  private var list: List[PartialFunction[I, R]] = List.empty

  def on[T <: I: ClassTag](f: T => R): FunctionBuilder[I, R, S] = {
    list ::= { case x: T ⇒ f(x) }
    this
  }

  def build(default: I ⇒ R): S = {
    val dd = list.reverse
      .reduceLeft(_ orElse _)
      .orElse {
        case x ⇒ default(x)
      }
      .lift
      .andThen(_.get)
    hof(dd)
  }
}

class FlatMapBuilder[I, R](input: Future[I])(implicit ec: ExecutionContext)
    extends FunctionBuilder[I, Future[R], Future[R]](x ⇒ input.flatMap(x))

class MapBuilder[I, R](input: Future[I])(implicit ec: ExecutionContext) extends FunctionBuilder[I, R, Future[R]](x ⇒ input.map(x))

object FlatMapBuilder {
  implicit class Ext[I](val input: Future[I]) extends AnyVal {
    def flatMapBuilder[R]: FlatMapBuilder[I, R] = new FlatMapBuilder(input)
    def mapBuilder[R]: MapBuilder[I, R]         = new MapBuilder(input)
  }
}

val longCommandResponse2 = Await.result(
        eventualResponse
          .flatMapBuilder[CommandResponse]
          .on[Accepted](x ⇒ assemblyRef.getCommandResponse(setupWithoutMatcher.runId))
          .on[Accepted](x ⇒ assemblyRef.getCommandResponse(setupWithoutMatcher.runId))
          .on[Accepted](x ⇒ assemblyRef.getCommandResponse(setupWithoutMatcher.runId))
          .build(default = x ⇒ Future.successful(x)),
        timeout.duration
      )
 */
