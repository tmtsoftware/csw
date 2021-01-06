package csw.params.events

import csw.prefix.models.Prefix
import enumeratum.{Enum, EnumEntry}

sealed trait WFSDetectorEvent extends EnumEntry

sealed trait WFSObserveEvent extends WFSDetectorEvent {
  def create(sourcePrefix: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

object WFSDetectorEvent extends Enum[WFSDetectorEvent] {
  override def values: IndexedSeq[WFSDetectorEvent] = findValues

  case object PublishSuccess extends WFSObserveEvent
  case object PublishFail    extends WFSObserveEvent
}

object JWFSDetectorEvent {
  val PublishSuccess = WFSDetectorEvent.PublishSuccess
  val PublishFail    = WFSDetectorEvent.PublishFail
}
