package csw.services.logging.internal

import TimeActorMessages.{TimeEnd, TimeStart}
import csw.services.logging.internal.LoggingState._
import csw.services.logging.scaladsl.RequestId

object MessageHandler {
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

  private[logging] def sendAkkaMsg(logAkka: LogAkka): Unit =
    if (logAkka.msg == "DIE") {
      akkaStopPromise.trySuccess(())
    } else {
      sendMsg(logAkka)
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
