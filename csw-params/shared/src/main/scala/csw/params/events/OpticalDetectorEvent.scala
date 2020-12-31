package csw.params.events

import csw.params.core.generics.KeyType.{LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import enumeratum.{Enum, EnumEntry}

sealed trait OpticalDetectorEvent extends EnumEntry

sealed trait OpticalObserveEvents extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

sealed trait OpticalObserveEventsWithExposureId extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

object OpticalDetectorEvent extends Enum[OpticalDetectorEvent] {
  override def values: IndexedSeq[OpticalDetectorEvent] = findValues

  case object ObserveStart extends OpticalObserveEvents
  case object ObserveEnd   extends OpticalObserveEvents

  case object PrepareStart    extends OpticalObserveEventsWithExposureId
  case object ExposureStart   extends OpticalObserveEventsWithExposureId
  case object ExposureEnd     extends OpticalObserveEventsWithExposureId
  case object ReadoutEnd      extends OpticalObserveEventsWithExposureId
  case object ReadoutFailed   extends OpticalObserveEventsWithExposureId
  case object DataWriteStart  extends OpticalObserveEventsWithExposureId
  case object DataWriteEnd    extends OpticalObserveEventsWithExposureId
  case object ExposureAborted extends OpticalObserveEventsWithExposureId

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
