package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix

private[events] class DetectorEvent(detectorExpStateName: EventName) {

  /**
   * This event indicates the start of execution of actions related to an Observe command.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeStart(sourcePrefix: Prefix, obsId: ObsId): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveStart, Some(obsId))

  /**
   * This event indicates the start of execution of actions related to an Observe command.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeStart(sourcePrefix: Prefix): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveStart)

  /**
   * This event indicates the end of execution of actions related  to an Observe command.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeEnd(sourcePrefix: Prefix, obsId: ObsId): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveEnd, Some(obsId))

  /**
   * This event indicates the end of execution of actions related  to an Observe command.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeEnd(sourcePrefix: Prefix): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveEnd)

  /**
   * This event indicates the start of data acquisition that  results in a file produced for DMS. This is a potential metadata event for DMS.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureStart(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ExposureStart)

  /**
   * This event indicates the end of data acquisition that results  in a file produced for DMS. This is a potential metadata event for DMS.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureEnd(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ExposureEnd)

  /**
   * This event indicates that a readout that is part of a ramp  has completed.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def readoutEnd(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ReadoutEnd)

  /**
   * This event indicates that a readout that is part of a ramp  has failed indicating transfer failure or some  other issue.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def readoutFailed(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ReadoutFailed)

  /**
   * This event indicates that the instrument has started writing  the exposure data file or transfer of exposure  data to DMS.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param filename   [[java.lang.String]] the path of the file.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def dataWriteStart(sourcePrefix: Prefix, exposureId: ExposureId, filename: String): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.DataWriteStart, filename)

  /**
   * This event indicates that the instrument has finished  writing the exposure data file or transfer of  exposure data to DMS.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param filename   [[java.lang.String]] the path of the file.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def dataWriteEnd(sourcePrefix: Prefix, exposureId: ExposureId, filename: String): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.DataWriteEnd, filename)

  /**
   * This event indicates that a request was made to abort the  exposure and it has completed. Normal data events should occur if data is  recoverable.
   * Abort should not fail
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureAborted(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ExposureAborted)

  /**
   * A state  variable to indicate the current state of the detector system.
   * The Exposure State Event groups  parameters that change relatively slowly, and
   * this event should be published whenever any of its  parameters changes.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param exposureInProgress [[java.lang.Boolean]] indicating if detector system is acquiring an exposure.
   *                           Delimited by exposureStart and exposureEnd. exposureInProgress should be false if abortInProgress is true (TBD)
   * @param abortInProgress [[java.lang.Boolean]] indicates that an abort has been requested and is underway.
   * @param isAborted [[java.lang.Boolean]] Indicates that an abort has occurred and is completed. abortInProgress should be false when isAborted is true.
   *                  isAborted should be set to false with the next exposure
   * @param errorMessage [[java.lang.String]] An parameter that can be included when the detector system  is in the ERROR operationalState.
   *                     This value should be cleared and removed from the state when the  operationalState returns to READY
   * @param operationalState [[csw.params.events.OperationalState]] indicating if the detector system is available and  operational.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureState(
      sourcePrefix: Prefix,
      exposureId: ExposureId,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      errorMessage: String,
      operationalState: OperationalState
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.exposureIdParam(exposureId),
      ParamFactories.operationalStateParam(operationalState),
      ParamFactories.errorMessageParam(errorMessage),
      ParamFactories.exposureInProgressParam(exposureInProgress),
      ParamFactories.abortInProgressParam(abortInProgress),
      ParamFactories.isAbortedParam(isAborted)
    )
    ObserveEvent(sourcePrefix, detectorExpStateName, params)
  }

  private def create(sourcePrefix: Prefix, eventName: EventName, obsId: Option[ObsId] = None): ObserveEvent =
    obsId match {
      case Some(value) => ObserveEvent(sourcePrefix, eventName, Set(ParamFactories.obsIdParam(value)))
      case None        => ObserveEvent(sourcePrefix, eventName, Set.empty)
    }

  private[events] def create(sourcePrefix: Prefix, exposureId: ExposureId, eventName: EventName): ObserveEvent =
    ObserveEvent(
      sourcePrefix,
      eventName,
      Set(ParamFactories.exposureIdParam(exposureId))
    )

  private[events] def create(
      sourcePrefix: Prefix,
      exposureId: ExposureId,
      eventName: EventName,
      filename: String
  ): ObserveEvent =
    ObserveEvent(
      sourcePrefix,
      eventName,
      Set(ParamFactories.exposureIdParam(exposureId), ParamFactories.filenameParam(filename))
    )
}
