package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

object OpticalDetectorEvent extends DetectorEvent(ObserveEventNames.OpticalDetectorExposureState) {
  def prepareStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.PrepareStart)

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
    ObserveEvent(Prefix(sourcePrefix), ObserveEventNames.OpticalDetectorExposureData, params)
  }
}
