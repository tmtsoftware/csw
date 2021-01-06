package csw.params.events

import csw.params.core.generics.KeyType.{LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

sealed trait OpticalDetectorEvent {
  protected def eventName: EventName = EventName(this.getClass.getSimpleName.dropRight(1))
}

sealed trait OpticalObserveEvent extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(StringKey.make("obsId").set(obsId.obsId))
    ObserveEvent(Prefix(sourcePrefix), (eventName), params)
  }
}

sealed trait OpticalObserveEventWithExposureId extends OpticalDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      StringKey.make("obsId").set(obsId.obsId),
      StringKey.make("exposureId").set(exposureId)
    )
    ObserveEvent(Prefix(sourcePrefix), (eventName), params)
  }
}

object OpticalDetectorEvent {
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
        StringKey.make("obsId").set(obsId.obsId),
        StringKey.make("detector").set(detector),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
      ObserveEvent(Prefix(sourcePrefix), eventName, params)
    }
  }
}

object JOpticalDetectorEvent {
  val ObserveStart                 = OpticalDetectorEvent.ObserveStart
  val ObserveEnd                   = OpticalDetectorEvent.ObserveEnd
  val PrepareStart                 = OpticalDetectorEvent.PrepareStart
  val ExposureStart                = OpticalDetectorEvent.ExposureStart
  val ExposureEnd                  = OpticalDetectorEvent.ExposureEnd
  val ReadoutEnd                   = OpticalDetectorEvent.ReadoutEnd
  val ReadoutFailed                = OpticalDetectorEvent.ReadoutFailed
  val DataWriteStart               = OpticalDetectorEvent.DataWriteStart
  val DataWriteEnd                 = OpticalDetectorEvent.DataWriteEnd
  val ExposureAborted              = OpticalDetectorEvent.ExposureAborted
  val OpticalDetectorExposureState = OpticalDetectorEvent.OpticalDetectorExposureState
  val OpticalDetectorExposureData  = OpticalDetectorEvent.OpticalDetectorExposureData
}
