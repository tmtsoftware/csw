package csw.params.events

object ObserveEventNames {
  // common
  val ObserveStart: EventName    = eventName("ObserveStart")
  val ObserveEnd: EventName      = eventName("ObserveEnd")
  val ExposureStart: EventName   = eventName("ExposureStart")
  val ExposureEnd: EventName     = eventName("ExposureEnd")
  val ReadoutEnd: EventName      = eventName("ReadoutEnd")
  val ReadoutFailed: EventName   = eventName("ReadoutFailed")
  val DataWriteStart: EventName  = eventName("DataWriteStart")
  val DataWriteEnd: EventName    = eventName("DataWriteEnd")
  val ExposureAborted: EventName = eventName("ExposureAborted")

  // IRDetector specific
  val IRDetectorExposureData: EventName  = eventName("IRDetectorExposureData")
  val IRDetectorExposureState: EventName = eventName("IRDetectorExposureState")

  // OpticalDetector specific
  val OpticalDetectorExposureData: EventName  = eventName("OpticalDetectorExposureData")
  val OpticalDetectorExposureState: EventName = eventName("OpticalDetectorExposureState")
  val PrepareStart: EventName                 = eventName("PrepareStart")

  // WFSDetector specific
  val WfsDetectorExposureState: EventName = eventName("WfsDetectorExposureState")
  val PublishSuccess: EventName           = eventName("PublishSuccess")
  val PublishFail: EventName              = eventName("PublishFail")

  private def eventName(name: String) = EventName(s"ObserveEvent.$name")
}
