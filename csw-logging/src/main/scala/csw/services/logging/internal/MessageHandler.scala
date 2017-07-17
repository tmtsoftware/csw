package csw.services.logging.internal

import TimeActorMessages.{TimeEnd, TimeStart}
import csw.services.logging.internal.LoggingState._
import csw.services.logging.scaladsl.{GenericLogger, RequestId}

/**
 * Acts as a single point of entry for messages from various loggers and redirects them to the log actor
 */
object MessageHandler extends GenericLogger.Simple {

  /**
   * Sends message to LogActor or maintains it in a queue till the log actor is not available
   * @param msg
   */
  private[logging] def sendMsg(msg: LogActorMessages): Unit =
    if (loggerStopping) {
      log.info(s"*** Log message received after logger shutdown: $msg")
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

  // Route time start messages to time actor
  private[logging] def timeStart(id: RequestId, name: String, uid: String): Unit =
    timeActorOption foreach { timeActor =>
      val time = System.nanoTime() / 1000
      timeActor ! TimeStart(id, name, uid, time)
    }

  // Route time end messages to time actor
  private[logging] def timeEnd(id: RequestId, name: String, uid: String): Unit =
    timeActorOption foreach { timeActor =>
      val time = System.nanoTime() / 1000
      timeActor ! TimeEnd(id, name, uid, time)
    }
}
