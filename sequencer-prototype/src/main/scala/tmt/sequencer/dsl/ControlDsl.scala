package tmt.sequencer.dsl

import tmt.sequencer.util.FutureExt.RichFuture
import tmt.sequencer.engine.Engine
import tmt.services.{Command, CommandResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait ControlDsl {
  def engine: Engine

  implicit def toFuture(x: => CommandResponse): Future[CommandResponse] = Future(x)

  def forEach(f: Command => Unit): Unit = {
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
