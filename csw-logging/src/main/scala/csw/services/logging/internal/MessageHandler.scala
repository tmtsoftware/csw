package csw.services.logging.internal

import csw.services.logging.internal.LogActorMessages.LogAkka
import csw.services.logging.internal.LoggingState._
import csw.services.logging.internal.TimeActorMessages.{TimeEnd, TimeStart}
import csw.services.logging.scaladsl.RequestId

/**
 * Acts as a single point of entry for messages from various loggers and redirects them to the log actor
 *
 *                      +-------------------------+                                    +------------------+
 *  --- akka logs ----> |     MessageHandler      |                                    |                  | --- forward to FileAppender ----->
 *                      |   (Singleton object)    |                                    |     LogActor     |
 *  --- slf4j logs ---> |                         | --- when LoggingSystem starts ---> |                  | --- forward to StdOutAppender --->
 *                      |   starts on jvm bootup  |                                    |                  |
 *  --- tmt logs -----> |                         |                                    |                  | --- forward to custom appender -->
 *                      | stores all logs in an   |                                    |                  |
 *                      | in-memory queue `msgs`  |                                    |                  |
 *                      +-------------------------+                                    +------------------+
 */
private[logging] object MessageHandler {

  /**
   * MessageHandler is a singleton object which will start at the bootup of jvm. If the `LoggingSystem` is not started
   * (which is also responsible for starting `LogActor`) then all the log messages from akka, slf4j, tmt will be enqueued
   * in an in-memory mutable.Queue `msgs`. This will result in log messages to NOT appear in file or stdout. Once, the
   * `LoggingSystem` is instantiated, `msgs` is emptied and log messages are processed by forwarding them to `LogActor`.
   * As an effect they will now start appearing in file or std out.
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
