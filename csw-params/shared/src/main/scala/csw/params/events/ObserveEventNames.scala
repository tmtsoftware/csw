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
  val PrepareStart: EventName    = eventName("PrepareStart")

  // IRDetector specific
  val IRDetectorExposureData: EventName  = eventName("IRDetectorExposureData")
  val IRDetectorExposureState: EventName = eventName("IRDetectorExposureState")

  // OpticalDetector specific
  val OpticalDetectorExposureData: EventName  = eventName("OpticalDetectorExposureData")
  val OpticalDetectorExposureState: EventName = eventName("OpticalDetectorExposureState")

  // WFSDetector specific
  val WfsDetectorExposureState: EventName = eventName("WfsDetectorExposureState")
  val PublishSuccess: EventName           = eventName("PublishSuccess")
  val PublishFail: EventName              = eventName("PublishFail")

  // Sequencer specific
  val PresetStart: EventName       = eventName("PresetStart")
  val PresetEnd: EventName         = eventName("PresetEnd")
  val GuidstarAcqStart: EventName  = eventName("GuidstarAcqStart")
  val GuidstarAcqEnd: EventName    = eventName("GuidstarAcqEnd")
  val ScitargetAcqStart: EventName = eventName("ScitargetAcqStart")
  val ScitargetAcqEnd: EventName   = eventName("ScitargetAcqEnd")
  val ObservationStart: EventName  = eventName("ObservationStart")
  val ObservationEnd: EventName    = eventName("ObservationEnd")
  val ObservePaused: EventName     = eventName("ObservePaused")
  val ObserveResumed: EventName    = eventName("ObserveResumed")
  val DowntimeStart: EventName     = eventName("DowntimeStart")

  private def eventName(name: String) = EventName(s"ObserveEvent.$name")
}
