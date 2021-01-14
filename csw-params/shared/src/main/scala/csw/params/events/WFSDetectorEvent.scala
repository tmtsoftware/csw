package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.events.IRDetectorEvent.observeEventPrefix

object WFSDetectorEvent {
  private def create(sourcePrefix: String, eventName: String): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.sourcePrefix(sourcePrefix)
    )
    ObserveEvent(observeEventPrefix, EventName(eventName), params)
  }

  def publishSuccess(sourcePrefix: String): ObserveEvent = create(sourcePrefix, "PublishSuccess")
  def publishFail(sourcePrefix: String): ObserveEvent    = create(sourcePrefix, "PublishFail")
  def exposureState(
      sourcePrefix: String,
      detector: String,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      operationalState: OperationalState,
      errorMessage: String
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      ParamFactories.sourcePrefix(sourcePrefix),
      ParamFactories.detectorParam(detector),
      ParamFactories.operationalStateParam(operationalState),
      ParamFactories.errorMessageParam(errorMessage),
      ParamFactories.exposureInProgressParam(exposureInProgress),
      ParamFactories.abortInProgressParam(abortInProgress),
      ParamFactories.isAbortedParam(isAborted)
    )
    ObserveEvent(observeEventPrefix, EventName("wfsDetectorExposureState"), params)
  }
}
