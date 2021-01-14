package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId

object IRDetectorEvent extends DetectorEvent("IRDetector") {
  def exposureData(
      sourcePrefix: String,
      obsId: ObsId,
      detector: String,
      readsInRamp: Int,
      readsComplete: Int,
      rampsInExposure: Int,
      rampsComplete: Int,
      exposureTime: Long,
      remainingExposureTime: Long
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.sourcePrefix(sourcePrefix),
      ParamFactories.obsIdParam(obsId),
      ParamFactories.detectorParam(detector),
      ParamFactories.readsInRampParam(readsInRamp),
      ParamFactories.readsCompleteParam(readsComplete),
      ParamFactories.rampsInExposureParam(rampsInExposure),
      ParamFactories.rampsCompleteParam(rampsComplete),
      ParamFactories.exposureTimeParam(exposureTime),
      ParamFactories.remainingExposureTimeParam(remainingExposureTime)
    )
    ObserveEvent(observeEventPrefix, EventName("IRDetectorExposureData"), params)
  }
}
