package csw.services.logging.internal

import csw.services.logging.internal.LoggingState._

/**
 * Acts as a single point of entry for messages from various loggers and redirects them to the log actor
 */
object MessageHandler {

  /**
   * Sends message to LogActor or maintains it in a queue till the log actor is not available
   * @param msg
   */
  private[logging] def sendMsg(msg: LogActorMessages): Unit =
    if (loggerStopping) {
//      println(s"*** Log message received after logger shutdown: $msg")
    } else {
      maybeLogActor match {
        case Some(logActor) => logActor ! msg
        case None =>
          msgs.synchronized {
            msgs.enqueue(msg)
          }
      }
    }

  // Route akka messages to common log actor
  private[logging] def sendAkkaMsg(logAkka: LogAkka): Unit =
    if (logAkka.msg == "DIE") {
      akkaStopPromise.trySuccess(())
    } else {
      sendMsg(logAkka)
    }
}
