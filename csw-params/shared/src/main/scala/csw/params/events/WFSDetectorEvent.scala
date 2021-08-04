package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ExposureId
import csw.prefix.models.Prefix

object WFSDetectorEvent {
  private def create(sourcePrefix: Prefix, eventName: EventName): ObserveEvent = ObserveEvent(sourcePrefix, eventName)

  def publishSuccess(sourcePrefix: Prefix): ObserveEvent = create(sourcePrefix, ObserveEventNames.PublishSuccess)
  def publishFail(sourcePrefix: Prefix): ObserveEvent    = create(sourcePrefix, ObserveEventNames.PublishFail)
  def exposureState(
      sourcePrefix: Prefix,
      exposureId: ExposureId,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      operationalState: OperationalState,
      errorMessage: String
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.exposureIdParam(exposureId),
      ParamFactories.operationalStateParam(operationalState),
      ParamFactories.errorMessageParam(errorMessage),
      ParamFactories.exposureInProgressParam(exposureInProgress),
      ParamFactories.abortInProgressParam(abortInProgress),
      ParamFactories.isAbortedParam(isAborted)
    )
    ObserveEvent(sourcePrefix, ObserveEventNames.WfsDetectorExposureState, params)
  }
}
