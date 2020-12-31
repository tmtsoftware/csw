package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.params.events.IRDetectorEvent.findValues
import csw.prefix.models.Prefix
import enumeratum.EnumEntry
import enumeratum.Enum

sealed trait OpticalDetectorEvent extends EnumEntry

sealed trait ObserveEvents extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

sealed trait ObserveEventsWithExposureId extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

object OpticalDetectorEvent extends Enum[OpticalDetectorEvent] {
  override def values: IndexedSeq[OpticalDetectorEvent] = findValues

  case object ObserveStart extends ObserveEvents
  case object ObserveEnd   extends ObserveEvents

  case object PrepareStart    extends ObserveEventsWithExposureId
  case object ExposureStart   extends ObserveEventsWithExposureId
  case object ExposureEnd     extends ObserveEventsWithExposureId
  case object ReadoutEnd      extends ObserveEventsWithExposureId
  case object ReadoutFailed   extends ObserveEventsWithExposureId
  case object DataWriteStart  extends ObserveEventsWithExposureId
  case object DataWriteEnd    extends ObserveEventsWithExposureId
  case object ExposureAborted extends ObserveEventsWithExposureId

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
