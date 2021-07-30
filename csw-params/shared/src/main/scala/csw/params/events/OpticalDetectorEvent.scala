package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureIdType, ObsId}
import csw.prefix.models.Prefix

object OpticalDetectorEvent extends DetectorEvent(ObserveEventNames.OpticalDetectorExposureState) {
  def prepareStart(sourcePrefix: Prefix, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.PrepareStart)

  def exposureData(
      sourcePrefix: Prefix,
      exposureId: ExposureIdType,
      exposureTime: Long,
      remainingExposureTime: Long
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.exposureIdParam(exposureId.toString),
      ParamFactories.exposureTimeParam(exposureTime),
      ParamFactories.remainingExposureTimeParam(remainingExposureTime)
    )
    ObserveEvent(sourcePrefix, ObserveEventNames.OpticalDetectorExposureData, params)
  }
}
