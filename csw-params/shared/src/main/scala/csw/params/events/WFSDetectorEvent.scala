package csw.params.events

import csw.prefix.models.Prefix

object WFSDetectorEvent {
  private def create(sourcePrefix: String, eventName: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(eventName))

  def publishSuccess(sourcePrefix: String): ObserveEvent = create(sourcePrefix, "PublishSuccess")
  def publishFail(sourcePrefix: String): ObserveEvent    = create(sourcePrefix, "PublishFail")
}
