package csw.params.events

import csw.params.Utils
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

object IRDetectorEvent extends ObserveEventFactories {
  def exposureState(
      sourcePrefix: String,
      obsId: ObsId,
      detector: String,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      errorMessage: String,
      operationalState: OperationalState
  ): ObserveEvent =
    createExposureState(
      sourcePrefix,
      obsId,
      detector,
      exposureInProgress,
      abortInProgress,
      isAborted,
      errorMessage,
      operationalState,
      "IRDetectorExposureState"
    )

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
      Utils.obsIdParam(obsId),
      Utils.detectorParam(detector),
      Utils.rampsCompleteParam(readsInRamp),
      Utils.rampsCompleteParam(readsComplete),
      Utils.rampsCompleteParam(rampsInExposure),
      Utils.rampsCompleteParam(rampsComplete),
      Utils.exposureTimeParam(exposureTime),
      Utils.remainingExposureTimeParam(remainingExposureTime)
    )
    ObserveEvent(Prefix(sourcePrefix), EventName("IRDetectorExposureData"), params)
  }
}
