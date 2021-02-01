package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

private[events] class DetectorEvent(detectorExpStateName: EventName) {

  def observeStart(sourcePrefix: String, obsId: ObsId): ObserveEvent = create(sourcePrefix, obsId, ObserveEventNames.ObserveStart)
  def observeEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent   = create(sourcePrefix, obsId, ObserveEventNames.ObserveEnd)

  def exposureStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ExposureStart)
  def exposureEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ExposureEnd)
  def readoutEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ReadoutEnd)
  def readoutFailed(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ReadoutFailed)
  def dataWriteStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.DataWriteStart)
  def dataWriteEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.DataWriteEnd)
  def exposureAborted(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, ObserveEventNames.ExposureAborted)

  def exposureState(
      sourcePrefix: String,
      obsId: ObsId,
      detector: String,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      errorMessage: String,
      operationalState: OperationalState
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.obsIdParam(obsId),
      ParamFactories.detectorParam(detector),
      ParamFactories.operationalStateParam(operationalState),
      ParamFactories.errorMessageParam(errorMessage),
      ParamFactories.exposureInProgressParam(exposureInProgress),
      ParamFactories.abortInProgressParam(abortInProgress),
      ParamFactories.isAbortedParam(isAborted)
    )
    ObserveEvent(Prefix(sourcePrefix), detectorExpStateName, params)
  }

  private def create(sourcePrefix: String, obsId: ObsId, eventName: EventName): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), eventName, Set(ParamFactories.obsIdParam(obsId)))

  private[events] def create(sourcePrefix: String, obsId: ObsId, exposureId: String, eventName: EventName): ObserveEvent =
    ObserveEvent(
      Prefix(sourcePrefix),
      eventName,
      Set(ParamFactories.obsIdParam(obsId), ParamFactories.exposureIdParam(exposureId))
    )
}
