package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

trait ObserveEventFactories {
  def observeStart(sourcePrefix: String, obsId: ObsId): ObserveEvent = create(sourcePrefix, obsId, "ObserveStart")
  def observeEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent   = create(sourcePrefix, obsId, "ObserveEnd")

  def exposureStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "ExposureStart")
  def exposureEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "ExposureEnd")
  def readoutEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "ReadoutEnd")
  def readoutFailed(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "ReadoutFailed")
  def dataWriteStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "DataWriteStart")
  def dataWriteEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "DataWriteEnd")
  def exposureAborted(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    create(sourcePrefix, obsId, exposureId, "ExposureAborted")

  private def create(sourcePrefix: String, obsId: ObsId, eventName: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(eventName), Set(ParamFactories.obsIdParam(obsId)))

  private[params] def create(sourcePrefix: String, obsId: ObsId, exposureId: String, eventName: String): ObserveEvent =
    ObserveEvent(
      Prefix(sourcePrefix),
      EventName(eventName),
      Set(ParamFactories.obsIdParam(obsId), ParamFactories.exposureIdParam(exposureId))
    )

  private[params] def createExposureState(
      sourcePrefix: String,
      obsId: ObsId,
      detector: String,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      errorMessage: String,
      operationalState: OperationalState,
      eventName: String
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
    ObserveEvent(Prefix(sourcePrefix), EventName(eventName), params)
  }
}
