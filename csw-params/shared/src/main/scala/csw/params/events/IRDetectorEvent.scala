package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ExposureId
import csw.prefix.models.Prefix

/**
 * The Observe Events for IR science detector systems.
 */
object IRDetectorEvent extends DetectorEvent(ObserveEventNames.IRDetectorExposureState) {

  /**
   * This event contains faster changing data about the internals of the current exposure. This data is useful for user interfaces  and Sequencers.
   * This event should be published at 1 Hz during an ongoing exposure.
   * This event does not have much information compared to the IR use case and is primarily for tracking  the remaining current exposure time in user interfaces or sequencers.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param readsInRamp The integer total number of reads in the ramp. Value should  be constant during an exposure.
   *                    (Note: for multi-array  detectors, it is assumed that all arrays work with the same  configuration).
   * @param readsComplete Integer number of current completed read from 1 to  readsInRamp. Should be reset to 0 at the start of every ramp
   * @param rampsInExposure  The integer total number of ramps in the current exposure. Value should be constant during an exposure.
   * @param rampsComplete  Integer number of completed ramp from 1 to rampsInExposure. Should be reset to 0 at the start of every exposure.
   * @param exposureTime Length in seconds of the current exposure
   * @param remainingExposureTime Number of seconds remaining in current exposure • Should count down in seconds – no faster than 1 Hz
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureData(
      sourcePrefix: Prefix,
      exposureId: ExposureId,
      readsInRamp: Int,
      readsComplete: Int,
      rampsInExposure: Int,
      rampsComplete: Int,
      exposureTime: Long,
      remainingExposureTime: Long
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.exposureIdParam(exposureId),
      ParamFactories.readsInRampParam(readsInRamp),
      ParamFactories.readsCompleteParam(readsComplete),
      ParamFactories.rampsInExposureParam(rampsInExposure),
      ParamFactories.rampsCompleteParam(rampsComplete),
      ParamFactories.exposureTimeParam(exposureTime),
      ParamFactories.remainingExposureTimeParam(remainingExposureTime)
    )
    ObserveEvent(sourcePrefix, ObserveEventNames.IRDetectorExposureData, params)
  }
}
