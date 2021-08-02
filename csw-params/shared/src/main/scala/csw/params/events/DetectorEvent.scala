package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureIdType, ObsId}
import csw.prefix.models.Prefix

private[events] class DetectorEvent(detectorExpStateName: EventName) {

  def observeStart(sourcePrefix: Prefix, obsId: ObsId): ObserveEvent = create(sourcePrefix, obsId, ObserveEventNames.ObserveStart)
  def observeEnd(sourcePrefix: Prefix, obsId: ObsId): ObserveEvent   = create(sourcePrefix, obsId, ObserveEventNames.ObserveEnd)

  def exposureStart(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ExposureStart)
  def exposureEnd(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ExposureEnd)
  def readoutEnd(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ReadoutEnd)
  def readoutFailed(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ReadoutFailed)
  def dataWriteStart(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.DataWriteStart)
  def dataWriteEnd(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.DataWriteEnd)
  def exposureAborted(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ExposureAborted)

  def exposureState(
      sourcePrefix: Prefix,
      exposureId: ExposureIdType,
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

  private def create(sourcePrefix: Prefix, obsId: ObsId, eventName: EventName): ObserveEvent =
    ObserveEvent(sourcePrefix, eventName, Set(ParamFactories.obsIdParam(obsId)))

  private[events] def create(sourcePrefix: Prefix, obsId: ObsId, exposureId: ExposureIdType, eventName: EventName): ObserveEvent =
    ObserveEvent(
      sourcePrefix,
      eventName,
      Set(ParamFactories.obsIdParam(obsId), ParamFactories.exposureIdParam(exposureId))
    )
}
