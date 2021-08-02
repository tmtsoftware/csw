package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, ChoiceKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.{GChoiceKey, Key, Parameter}
import csw.params.core.models.{Choice, Choices, ExposureIdType, ObsId}

object ObserveEventKeys {

  val obsId: Key[String]      = StringKey.make("obsId")
  val exposureId: Key[String] = StringKey.make("exposureId")
  val detector: Key[String]   = StringKey.make("detector")
  val operationalState: GChoiceKey = ChoiceKey
    .make("operationalState", Choices.fromChoices(OperationalState.toChoices: _*))
  val errorMessage: Key[String]        = StringKey.make("errorMessage")
  val exposureInProgress: Key[Boolean] = BooleanKey.make("exposureInProgress")
  val abortInProgress: Key[Boolean]    = BooleanKey.make("abortInProgress")
  val isAborted: Key[Boolean]          = BooleanKey.make("isAborted")
  val exposureTime: Key[Long]          = LongKey.make("exposureTime")
  val remainingExposureTime: Key[Long] = LongKey.make("remainingExposureTime")
  val readsInRamp                      = IntKey.make("readsInRamp")
  val readsComplete: Key[Int]          = IntKey.make("readsComplete")
  val rampsInExposure: Key[Int]        = IntKey.make("rampsInExposure")
  val rampsComplete: Key[Int]          = IntKey.make("rampsComplete")
  val downTimeReason: Key[String]      = StringKey.make("reason")

}

// commonly used params factories
object ParamFactories {
  def obsIdParam(obsId: ObsId): Parameter[String] = ObserveEventKeys.obsId.set(obsId.toString)

  def exposureIdParam(exposureId: ExposureIdType): Parameter[String] = ObserveEventKeys.exposureId.set(exposureId.toString)

  def detectorParam(detector: String): Parameter[String] = ObserveEventKeys.detector.set(detector)

  def operationalStateParam(operationalState: OperationalState): Parameter[Choice] =
    ObserveEventKeys.operationalState
      .set(Choice(operationalState.entryName))

  def errorMessageParam(errorMessage: String): Parameter[String] = ObserveEventKeys.errorMessage.set(errorMessage)

  def exposureInProgressParam(exposureInProgress: Boolean): Parameter[Boolean] =
    ObserveEventKeys.exposureInProgress.set(exposureInProgress)

  def abortInProgressParam(abortInProgress: Boolean): Parameter[Boolean] = ObserveEventKeys.abortInProgress.set(abortInProgress)

  def isAbortedParam(isAborted: Boolean): Parameter[Boolean] = ObserveEventKeys.isAborted.set(isAborted)

  def exposureTimeParam(exposureTime: Long): Parameter[Long] = ObserveEventKeys.exposureTime.set(exposureTime)

  def remainingExposureTimeParam(remainingExposureTime: Long): Parameter[Long] =
    ObserveEventKeys.remainingExposureTime.set(remainingExposureTime)

  def readsInRampParam(readsInRamp: Int): Parameter[Int] = ObserveEventKeys.readsInRamp.set(readsInRamp)

  def readsCompleteParam(readsComplete: Int): Parameter[Int] = ObserveEventKeys.readsComplete.set(readsComplete)

  def rampsInExposureParam(rampsInExposure: Int): Parameter[Int] = ObserveEventKeys.rampsInExposure.set(rampsInExposure)

  def rampsCompleteParam(rampsComplete: Int): Parameter[Int] = ObserveEventKeys.rampsComplete.set(rampsComplete)

  def downTimeReasonParam(reasonForDownTime: String): Parameter[String] = ObserveEventKeys.downTimeReason.set(reasonForDownTime)
}
