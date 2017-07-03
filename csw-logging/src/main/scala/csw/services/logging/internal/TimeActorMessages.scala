package csw.services.logging.internal

import csw.services.logging.scaladsl.RequestId

private[logging] object TimeActorMessages {

  private[logging] trait TimeActorMessage

  private[logging] case class TimeStart(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  private[logging] case class TimeEnd(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  private[logging] case object TimeDone extends TimeActorMessage

}
