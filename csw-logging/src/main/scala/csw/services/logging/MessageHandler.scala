package csw.services.logging

import csw.services.logging.LogActor.{AkkaMessage, LogActorMessage}
import csw.services.logging.LoggingState._
import csw.services.logging.TimeActorMessages.{TimeEnd, TimeStart}

object MessageHandler {
  private[logging] def sendMsg(msg: LogActorMessage): Unit =
    if (loggerStopping) {
      println(s"*** Log message received after logger shutdown: $msg")
    } else {
      logActor match {
        case Some(a) =>
          a ! msg
        case None =>
          msgs.synchronized {
            msgs.enqueue(msg)
          }
      }
    }

  private[logging] def akkaMsg(m: AkkaMessage): Unit =
    if (m.msg == "DIE") {
      akkaStopPromise.trySuccess(())
    } else {
      sendMsg(m)
    }

  private[logging] def timeStart(id: RequestId, name: String, uid: String): Unit =
    timeActorOption foreach { timeActor =>
      val time = System.nanoTime() / 1000
      timeActor ! TimeStart(id, name, uid, time)
    }

  private[logging] def timeEnd(id: RequestId, name: String, uid: String): Unit =
    timeActorOption foreach { timeActor =>
      val time = System.nanoTime() / 1000
      timeActor ! TimeEnd(id, name, uid, time)
    }
}
