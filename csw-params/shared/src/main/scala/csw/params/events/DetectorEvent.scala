package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix

private[events] class DetectorEvent(detectorExpStateName: EventName) {

  def observeStart(sourcePrefix: Prefix, obsId: ObsId): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveStart, Some(obsId))
  def observeStart(sourcePrefix: Prefix): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveStart)

  def observeEnd(sourcePrefix: Prefix, obsId: ObsId): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveEnd, Some(obsId))
  def observeEnd(sourcePrefix: Prefix): ObserveEvent =
    create(sourcePrefix, ObserveEventNames.ObserveEnd)

  def exposureStart(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ExposureStart)
  def exposureEnd(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ExposureEnd)
  def readoutEnd(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ReadoutEnd)
  def readoutFailed(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ReadoutFailed)
  def dataWriteStart(sourcePrefix: Prefix, exposureId: ExposureId, filename: String): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.DataWriteStart, filename)
  def dataWriteEnd(sourcePrefix: Prefix, exposureId: ExposureId, filename: String): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.DataWriteEnd, filename)
  def exposureAborted(sourcePrefix: Prefix, exposureId: ExposureId): ObserveEvent =
    create(sourcePrefix, exposureId, ObserveEventNames.ExposureAborted)

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
