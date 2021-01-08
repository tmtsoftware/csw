package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

object OpticalDetectorEvent extends ObserveEventFactories {
  def prepareStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "PrepareStart")

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
      "OpticalDetectorExposureState"
    )

  def exposureData(
      sourcePrefix: String,
      obsId: ObsId,
      detector: String,
      exposureTime: Long,
      remainingExposureTime: Long
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.obsIdParam(obsId),
      ParamFactories.detectorParam(detector),
      ParamFactories.exposureTimeParam(exposureTime),
      ParamFactories.remainingExposureTimeParam(remainingExposureTime)
    )
    ObserveEvent(Prefix(sourcePrefix), EventName("OpticalDetectorExposureData"), params)
  }
}
