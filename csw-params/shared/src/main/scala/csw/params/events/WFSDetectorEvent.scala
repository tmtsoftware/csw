package csw.params.events

import csw.prefix.models.Prefix

sealed trait WFSDetectorEvent {

  protected def eventName: EventName = {
    val simpleName = this.getClass.getSimpleName
    EventName(if (simpleName.last == '$') simpleName.dropRight(1) else simpleName)
  }

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
