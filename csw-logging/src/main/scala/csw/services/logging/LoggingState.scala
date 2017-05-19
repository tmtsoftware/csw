package csw.services.logging

import akka.actor._
import LogActor.{AkkaMessage, LogActorMessage}
import scala.language.existentials
import scala.concurrent.Promise
import scala.collection.mutable
import TimeActorMessages._

/**
 * Global state info for logging. Use with care!
 */
private[logging] object LoggingState extends ClassLogging {

  // Queue of messages sent before logger is started
  private[logging] val msgs = new mutable.Queue[LogActorMessage]()

  @volatile var doTrace: Boolean = false
  @volatile var doDebug: Boolean = false
  @volatile var doInfo: Boolean  = true
  @volatile var doWarn: Boolean  = true
  @volatile var doError: Boolean = true

  private[logging] var loggingSys: LoggingSystem = null

  private[logging] var logger: Option[ActorRef] = None
  @volatile private[logging] var loggerStopping = false

  private[logging] var doTime: Boolean                   = false
  private[logging] var timeActorOption: Option[ActorRef] = None

  // Use to sync akka logging actor shutdown
  private[logging] val akkaStopPromise = Promise[Unit]

  private[logging] def sendMsg(msg: LogActorMessage) {
    if (loggerStopping) {
      println(s"*** Log message received after logger shutdown: $msg")
    } else {
      logger match {
        case Some(a) =>
          a ! msg
        case None =>
          msgs.synchronized {
            msgs.enqueue(msg)
          }
      }
    }
  }

  private[logging] def akkaMsg(m: AkkaMessage) {
    if (m.msg == "DIE") {
      akkaStopPromise.trySuccess(())
    } else {
      sendMsg(m)
    }
  }

  private[logging] def timeStart(id: RequestId, name: String, uid: String) {
    timeActorOption foreach {
      case timeActor =>
        val time = System.nanoTime() / 1000
        timeActor ! TimeStart(id, name, uid, time)
    }
  }

  private[logging] def timeEnd(id: RequestId, name: String, uid: String) {
    timeActorOption foreach {
      case timeActor =>
        val time = System.nanoTime() / 1000
        timeActor ! TimeEnd(id, name, uid, time)
    }
  }
}
