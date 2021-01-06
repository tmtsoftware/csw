package csw.params.events

import csw.prefix.models.Prefix

sealed trait WFSDetectorEvent {
  protected def eventName: EventName = EventName(this.getClass.getSimpleName.dropRight(1))

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
