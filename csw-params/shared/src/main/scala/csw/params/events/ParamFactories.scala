package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId

object ParamFactories {
  // commonly used params factories
  def obsIdParam(obsId: ObsId): Parameter[String]            = StringKey.make("obsId").set(obsId.toString)
  def exposureIdParam(exposureId: String): Parameter[String] = StringKey.make("exposureId").set(exposureId)
  def detectorParam(detector: String): Parameter[String]     = StringKey.make("detector").set(detector)
  def operationalStateParam(operationalState: OperationalState): Parameter[String] =
    StringKey.make("operationalState").set(operationalState.toString)
  def errorMessageParam(errorMessage: String): Parameter[String] = StringKey.make("errorMessage").set(errorMessage)
  def exposureInProgressParam(exposureInProgress: Boolean): Parameter[Boolean] =
    BooleanKey.make("exposureInProgress").set(exposureInProgress)
  def abortInProgressParam(abortInProgress: Boolean): Parameter[Boolean] = BooleanKey.make("abortInProgress").set(abortInProgress)
  def isAbortedParam(isAborted: Boolean): Parameter[Boolean]             = BooleanKey.make("isAborted").set(isAborted)

  def exposureTimeParam(exposureTime: Long): Parameter[Long] = LongKey.make("exposureTime").set(exposureTime)
  def remainingExposureTimeParam(remainingExposureTime: Long): Parameter[Long] =
    LongKey.make("remainingExposureTime").set(remainingExposureTime)

  def readsInRampParam(readsInRamp: Int): Parameter[Int]         = IntKey.make("readsInRamp").set(readsInRamp)
  def readsCompleteParam(readsComplete: Int): Parameter[Int]     = IntKey.make("readsComplete").set(readsComplete)
  def rampsInExposureParam(rampsInExposure: Int): Parameter[Int] = IntKey.make("rampsInExposure").set(rampsInExposure)
  def rampsCompleteParam(rampsComplete: Int): Parameter[Int]     = IntKey.make("rampsComplete").set(rampsComplete)
}
