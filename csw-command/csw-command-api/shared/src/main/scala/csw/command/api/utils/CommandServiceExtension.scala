package csw.command.api.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.scaladsl.CommandService
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.states.CurrentState
import msocket.portable.PortableAkka

import scala.async.Async._
import scala.concurrent.{Future, Promise, TimeoutException}
import scala.util.{Failure, Success}

class CommandServiceExtension(commandService: CommandService)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def submitAndWait(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] = {
    commandService.submit(controlCommand).flatMap {
      case started: Started => commandService.queryFinal(started.runId)
      case x                => Future.successful(x)
    }
  }

  def submitAllAndWait(submitCommands: List[ControlCommand])(implicit timeout: Timeout): Future[List[SubmitResponse]] = {
    async {
      val iterator                             = submitCommands.iterator
      var result: List[SubmitResponse]         = Nil
      var lastResponse: Option[SubmitResponse] = None
      while (iterator.hasNext && lastResponse.forall(x => !isNegative(x))) {
        val response = await(submitAndWait(iterator.next()))
        result ::= response
        lastResponse = Some(response)
      }
      result.reverse
    }
  }

  def onewayAndMatch(controlCommand: ControlCommand, stateMatcher: StateMatcher): Future[MatchingResponse] = {
    val p: Promise[CurrentState] = Promise()

    val subscription = commandService.subscribeCurrentState { cs =>
      if (cs.stateName == stateMatcher.stateName && cs.prefix == stateMatcher.prefix && stateMatcher.check(cs)) {
        p.trySuccess(cs)
      }
    }

    PortableAkka.setTimeout(stateMatcher.timeout.duration) {
      p.tryFailure(new TimeoutException(s"matching could not be done within ${stateMatcher.timeout.duration}"))
    }

    p.future.onComplete(_ => subscription.cancel())

    commandService.oneway(controlCommand).flatMap {
      case Accepted(runId) =>
        p.future.transform {
          case Success(_)  => Success(Completed(runId))
          case Failure(ex) => Success(Error(runId, ex.getMessage))
        }
      case x @ _ => Future.successful(x.asInstanceOf[MatchingResponse])
    }
  }

}
