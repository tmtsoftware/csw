package csw.logging.client.internal

import csw.logging.api.models.RequestId

private[logging] object TimeActorMessages {

  trait TimeActorMessage

  case class TimeStart(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  case class TimeEnd(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  case object TimeDone extends TimeActorMessage

}
