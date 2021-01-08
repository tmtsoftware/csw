package csw.params.events

import csw.params.Utils
import csw.prefix.models.Prefix

sealed trait WFSDetectorEvent {

  protected def eventName: EventName = EventName(Utils.getClassName(this))

  def create(sourcePrefix: String): ObserveEvent = ObserveEvent(Prefix(sourcePrefix), eventName)
}

object WFSDetectorEvent {
  case object PublishSuccess extends WFSDetectorEvent
  case object PublishFail    extends WFSDetectorEvent
}

object JWFSDetectorEvent {
  val PublishSuccess = WFSDetectorEvent.PublishSuccess
  val PublishFail    = WFSDetectorEvent.PublishFail
}
