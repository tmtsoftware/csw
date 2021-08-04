package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix

object OpticalDetectorEvent extends DetectorEvent(ObserveEventNames.OpticalDetectorExposureState) {
  def prepareStart(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.PrepareStart)

  def exposureData(
      sourcePrefix: Prefix,
      exposureId: ExposureId,
      exposureTime: Long,
      remainingExposureTime: Long
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.exposureIdParam(exposureId),
      ParamFactories.exposureTimeParam(exposureTime),
      ParamFactories.remainingExposureTimeParam(remainingExposureTime)
    )
    ObserveEvent(sourcePrefix, ObserveEventNames.OpticalDetectorExposureData, params)
  }
}
