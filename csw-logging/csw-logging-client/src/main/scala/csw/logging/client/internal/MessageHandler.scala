/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.internal

import csw.logging.client.internal.LogActorMessages.LogPekko
import csw.logging.client.internal.LoggingState._
import csw.logging.client.internal.TimeActorMessages.{TimeEnd, TimeStart}
import csw.logging.models.RequestId

/**
 * Acts as a single point of entry for messages from various loggers and redirects them to the log actor
 *
 *                      +-------------------------+                                    +------------------+
 *  --- pekko logs ----> |     MessageHandler      |                                    |                  | --- forward to FileAppender ----->
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
   * (which is also responsible for starting `LogActor`) then all the log messages from pekko, slf4j, tmt will be enqueued
   * in an in-memory mutable.Queue `msgs`. This will result in log messages to NOT appear in file or stdout. Once, the
   * `LoggingSystem` is instantiated, `msgs` is emptied and log messages are processed by forwarding them to `LogActor`.
   * As an effect they will now start appearing in file or std out.
   */
  private[logging] def sendMsg(msg: LogActorMessages): Unit =
    if (loggerStopping) {
//      println(s"*** Log message received after logger shutdown: $msg")
    }
    else {
      maybeLogActor.foreach(_ ! msg)
    }

  // Route pekko messages to common log actor
  private[logging] def sendPekkoMsg(logPekko: LogPekko): Unit =
    if (logPekko.msg == "DIE") pekkoStopPromise.trySuccess(())
    else sendMsg(logPekko)

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
