package csw.params.events

import csw.params.core.generics.KeyType.{LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import enumeratum.{Enum, EnumEntry}

sealed trait OpticalDetectorEvent extends EnumEntry

sealed trait OpticalObserveEvent extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

sealed trait OpticalObserveEventWithExposureId extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

object OpticalDetectorEvent extends Enum[OpticalDetectorEvent] {
  override def values: IndexedSeq[OpticalDetectorEvent] = findValues

  case object ObserveStart extends OpticalObserveEvent
  case object ObserveEnd   extends OpticalObserveEvent

  case object PrepareStart    extends OpticalObserveEventWithExposureId
  case object ExposureStart   extends OpticalObserveEventWithExposureId
  case object ExposureEnd     extends OpticalObserveEventWithExposureId
  case object ReadoutEnd      extends OpticalObserveEventWithExposureId
  case object ReadoutFailed   extends OpticalObserveEventWithExposureId
  case object DataWriteStart  extends OpticalObserveEventWithExposureId
  case object DataWriteEnd    extends OpticalObserveEventWithExposureId
  case object ExposureAborted extends OpticalObserveEventWithExposureId

  case object OpticalDetectorExposureState extends OpticalDetectorEvent with ExposureState

  case object OpticalDetectorExposureData extends OpticalDetectorEvent {
    def create(
        sourcePrefix: String,
        obsId: ObsId,
        detector: String,
        exposureTime: Long,
        remainingExposureTime: Long
    ): ObserveEvent = {
      val params: Set[Parameter[_]] = Set(
        StringKey.make("detector").set(detector),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
      ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName), params)
    }
  }

}
