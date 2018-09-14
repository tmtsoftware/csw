package csw.alarm.cli.extensions

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object RichFutureExt {
  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def transformWithSideEffect(printLine: Any ⇒ Unit)(implicit ex: ExecutionContext): Future[T] = f transform {
      case x: Success[T] ⇒ printLine("[SUCCESS] Command executed successfully."); x
      case x: Failure[T] ⇒ printLine("[FAILURE] Failed to execute the command."); x
    }
  }
}
