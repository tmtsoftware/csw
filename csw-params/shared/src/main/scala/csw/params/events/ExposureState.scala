package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import enumeratum.EnumEntry

trait ExposureState extends EnumEntry {
  def create(
      sourcePrefix: String,
      obsId: ObsId,
      detector: String,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      errorMessage: String,
      operationalState: OperationalState.Value
  ): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      StringKey.make("detector").set(detector),
      StringKey.make("operationalState").set(operationalState.toString),
      StringKey.make("errorMessage").set(errorMessage),
      BooleanKey.make("exposureInProgress").set(exposureInProgress),
      BooleanKey.make("abortInProgress").set(abortInProgress),
      BooleanKey.make("isAborted").set(isAborted)
    )
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName), params)
  }
}
