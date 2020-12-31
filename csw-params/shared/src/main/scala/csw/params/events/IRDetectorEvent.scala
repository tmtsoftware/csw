package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import enumeratum.EnumEntry
import enumeratum.Enum

sealed trait IRDetectorEvent extends EnumEntry

sealed trait ObserveEvents extends IRDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

sealed trait ObserveEventsWithExposureId extends IRDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

object IRDetectorEvent extends Enum[IRDetectorEvent] {
  override def values: IndexedSeq[IRDetectorEvent] = findValues

  case object ObserveStart extends ObserveEvents
  case object ObserveEnd   extends ObserveEvents

  case object ExposureStart   extends ObserveEventsWithExposureId
  case object ExposureEnd     extends ObserveEventsWithExposureId
  case object ReadoutEnd      extends ObserveEventsWithExposureId
  case object ReadoutFailed   extends ObserveEventsWithExposureId
  case object DataWriteStart  extends ObserveEventsWithExposureId
  case object DataWriteEnd    extends ObserveEventsWithExposureId
  case object ExposureAborted extends ObserveEventsWithExposureId

  case object IrDetectorExposureState extends IRDetectorEvent with ExposureState

  case object IrDetectorExposureData extends IRDetectorEvent {
    def create(
        sourcePrefix: String,
        obsId: ObsId,
        detector: String,
        readsInRamp: Int,
        readsComplete: Int,
        rampsInExposure: Int,
        rampsComplete: Int,
        exposureTime: Long,
        remainingExposureTime: Long
    ): ObserveEvent = {
      val params: Set[Parameter[_]] = Set(
        StringKey.make("detector").set(detector),
        IntKey.make("readsInRamp").set(readsInRamp),
        IntKey.make("readsComplete").set(readsComplete),
        IntKey.make("rampsInExposure").set(rampsInExposure),
        IntKey.make("rampsComplete").set(rampsComplete),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
      ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName), params)
    }
  }
}
