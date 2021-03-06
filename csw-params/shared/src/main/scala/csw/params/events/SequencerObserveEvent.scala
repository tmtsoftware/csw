package csw.params.events

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureIdType, ObsId}
import csw.prefix.models.Prefix

case class SequencerObserveEvent(prefix: Prefix) {

  def presetStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.PresetStart, obsId)

  def presetEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.PresetEnd, obsId)

  def guidstarAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.GuidstarAcqStart, obsId)

  def guidstarAcqEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.GuidstarAcqEnd, obsId)

  def scitargetAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ScitargetAcqStart, obsId)

  def scitargetAcqEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ScitargetAcqEnd, obsId)

  def observationStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObservationStart, obsId)

  def observationEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObservationEnd, obsId)

  def observeStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObserveStart, obsId)

  def observeEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObserveEnd, obsId)

  def exposureStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.ExposureStart, obsId, exposureId)

  def exposureEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.ExposureEnd, obsId, exposureId)

  def readoutEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.ReadoutEnd, obsId, exposureId)

  def readoutFailed(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.ReadoutFailed, obsId, exposureId)

  def dataWriteStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.DataWriteStart, obsId, exposureId)

  def dataWriteEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.DataWriteEnd, obsId, exposureId)

  def prepareStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(ObserveEventNames.PrepareStart, obsId, exposureId)

  def observePaused(): ObserveEvent = ObserveEvent(prefix, ObserveEventNames.ObservePaused)

  def observeResumed(): ObserveEvent = ObserveEvent(prefix, ObserveEventNames.ObserveResumed)

  def downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = ParamFactories.obsIdParam(obsId)
    val downtimeReasonParam = ParamFactories.downTimeReasonParam(reasonForDowntime)
    ObserveEvent(prefix, ObserveEventNames.DowntimeStart, Set(obsIdParam, downtimeReasonParam))
  }

  private def createObserveEvent(eventName: EventName, obsId: ObsId) =
    ObserveEvent(prefix, eventName, Set(StringKey.make("obsId").set(obsId.toString())))

  private def createObserveEventWithExposureId(eventName: EventName, obsId: ObsId, exposureId: ExposureIdType) = {
    val paramset: Set[Parameter[_]] = Set(
      ParamFactories.obsIdParam(obsId),
      ParamFactories.exposureIdParam(exposureId.toString)
    )
    ObserveEvent(prefix, eventName, paramset)
  }
}
