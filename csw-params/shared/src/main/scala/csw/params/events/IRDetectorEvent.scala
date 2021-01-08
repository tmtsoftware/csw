package csw.params.events

import csw.params.core.generics.KeyType.{IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

sealed trait IRDetectorEvent {
  protected def eventName: EventName = {
    val simpleName = this.getClass.getSimpleName
    EventName(if (simpleName.last == '$') simpleName.dropRight(1) else simpleName)
  }
}

sealed trait IRObserveEvent extends IRDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(StringKey.make("obsId").set(obsId.obsId))
    ObserveEvent(Prefix(sourcePrefix), eventName, params)
  }
}

sealed trait IRObserveEventWithExposureId extends IRDetectorEvent {
  def create(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent = {
    val params: Set[Parameter[_]] = Set(
      StringKey.make("obsId").set(obsId.obsId),
      StringKey.make("exposureId").set(exposureId)
    )
    ObserveEvent(Prefix(sourcePrefix), eventName, params)
  }
}

object IRDetectorEvent {

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
        StringKey.make("obsId").set(obsId.obsId),
        StringKey.make("detector").set(detector),
        IntKey.make("readsInRamp").set(readsInRamp),
        IntKey.make("readsComplete").set(readsComplete),
        IntKey.make("rampsInExposure").set(rampsInExposure),
        IntKey.make("rampsComplete").set(rampsComplete),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
      ObserveEvent(Prefix(sourcePrefix), eventName, params)
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
