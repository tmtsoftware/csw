package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

trait ExposureState {
  def create(
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
      StringKey.make("obsId").set(obsId.obsId),
      StringKey.make("detector").set(detector),
      StringKey.make("operationalState").set(operationalState.toString),
      StringKey.make("errorMessage").set(errorMessage),
      BooleanKey.make("exposureInProgress").set(exposureInProgress),
      BooleanKey.make("abortInProgress").set(abortInProgress),
      BooleanKey.make("isAborted").set(isAborted)
    )
    ObserveEvent(Prefix(sourcePrefix), EventName(name), params)
  }

  private def name = {
    val simpleName = getClass.getSimpleName
    if (simpleName.last == '$') simpleName.dropRight(1) else simpleName
  }

}
