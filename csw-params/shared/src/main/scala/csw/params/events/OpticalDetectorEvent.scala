package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ExposureId
import csw.prefix.models.Prefix

/**
 * The optical science detector system Observe Events
 */
object OpticalDetectorEvent extends DetectorEvent(ObserveEventNames.OpticalDetectorExposureState) {
  def prepareStart(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.PrepareStart)

  /**
   * This event contains faster changing data about the internals of the current exposure. This data is useful for user interfaces  and Sequencers.
   * This event should be published at 1 Hz during an ongoing exposure.
   * This event does not have much information compared to the IR use case and is primarily for tracking  the remaining current exposure time in user interfaces or sequencers.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param exposureTime Length in seconds of the current exposure
   * @param remainingExposureTime Number of seconds remaining in current exposure • Should count down in seconds – no faster than 1 Hz
   * @return [[csw.params.events.ObserveEvent]]]
   */
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
