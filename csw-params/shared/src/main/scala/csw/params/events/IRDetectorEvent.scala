package csw.params.events

import csw.params.core.generics.KeyType.{IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import enumeratum.{Enum, EnumEntry}

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

  case object IRDetectorExposureState extends IRDetectorEvent with ExposureState

  case object IRDetectorExposureData extends IRDetectorEvent {
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

object JIRDetectorEvent {
  val ObserveStart            = IRDetectorEvent.ObserveStart
  val ObserveEnd              = IRDetectorEvent.ObserveEnd
  val ExposureStart           = IRDetectorEvent.ExposureStart
  val ExposureEnd             = IRDetectorEvent.ExposureEnd
  val ReadoutEnd              = IRDetectorEvent.ReadoutEnd
  val ReadoutFailed           = IRDetectorEvent.ReadoutFailed
  val DataWriteStart          = IRDetectorEvent.DataWriteStart
  val DataWriteEnd            = IRDetectorEvent.DataWriteEnd
  val ExposureAborted         = IRDetectorEvent.ExposureAborted
  val IRDetectorExposureState = IRDetectorEvent.IRDetectorExposureState
  val IRDetectorExposureData  = IRDetectorEvent.IRDetectorExposureData
}
