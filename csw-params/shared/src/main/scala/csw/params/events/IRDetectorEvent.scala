package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import enumeratum.EnumEntry
import enumeratum.Enum

sealed trait IRDetectorEvent extends EnumEntry

sealed trait IRObserveEvent extends IRDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

sealed trait IRObserveEventWithExposureId extends IRDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.entryName))
}

object IRDetectorEvent extends Enum[IRDetectorEvent] {
  override def values: IndexedSeq[IRDetectorEvent] = findValues

  case object ObserveStart extends IRObserveEvent
  case object ObserveEnd   extends IRObserveEvent

  case object ExposureStart   extends IRObserveEventWithExposureId
  case object ExposureEnd     extends IRObserveEventWithExposureId
  case object ReadoutEnd      extends IRObserveEventWithExposureId
  case object ReadoutFailed   extends IRObserveEventWithExposureId
  case object DataWriteStart  extends IRObserveEventWithExposureId
  case object DataWriteEnd    extends IRObserveEventWithExposureId
  case object ExposureAborted extends IRObserveEventWithExposureId

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
