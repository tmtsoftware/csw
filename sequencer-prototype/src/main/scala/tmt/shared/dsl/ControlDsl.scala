package tmt.shared.dsl

import csw.messages.ccs.commands.ControlCommand
import tmt.shared.util.FutureExt.RichFuture
import tmt.shared.services.{Command, CommandResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait ControlDsl {
  def engine: EngineDsl

  implicit def toFuture(x: => CommandResponse): Future[CommandResponse] = Future(x)

  def forEach(f: ControlCommand => Unit): Unit = {
    loop(f(engine.pullNext()))
  }

  def loop(block: => Unit): Unit = Future {
    while (true) {
      block
    }
  }

  def par(fs: Future[CommandResponse]*): Seq[CommandResponse] = Future.sequence(fs.toList).await

  implicit class RichCommandResponse(commandResponse: => CommandResponse) {
    def async: Future[CommandResponse] = Future(commandResponse)
  }
}
