package csw.params.events

import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix

case class SequencerObserveEvent(prefix: Prefix) {

  def presetStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.PresetStart, obsId)

  def presetEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.PresetEnd, obsId)

  def guidestarAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.GuidestarAcqStart, obsId)

  def guidestarAcqEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.GuidestarAcqEnd, obsId)

  def scitargetAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ScitargetAcqStart, obsId)

  def scitargetAcqEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ScitargetAcqEnd, obsId)

  def observationStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObservationStart, obsId)

  def observationEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObservationEnd, obsId)

  def observeStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObserveStart, obsId)

  def observeEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObserveEnd, obsId)

  def exposureStart(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ExposureStart, exposureId)

  def exposureEnd(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ExposureEnd, exposureId)

  def readoutEnd(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ReadoutEnd, exposureId)

  def readoutFailed(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ReadoutFailed, exposureId)

  def dataWriteStart(exposureId: ExposureId, filename: String): ObserveEvent =
    createObserveEvent(ObserveEventNames.DataWriteStart, exposureId, filename)

  def dataWriteEnd(exposureId: ExposureId, filename: String): ObserveEvent =
    createObserveEvent(ObserveEventNames.DataWriteEnd, exposureId, filename)

  def prepareStart(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.PrepareStart, exposureId)

  def observePaused(): ObserveEvent = ObserveEvent(prefix, ObserveEventNames.ObservePaused)

  def observeResumed(): ObserveEvent = ObserveEvent(prefix, ObserveEventNames.ObserveResumed)

  def downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = ParamFactories.obsIdParam(obsId)
    val downtimeReasonParam = ParamFactories.downTimeReasonParam(reasonForDowntime)
    ObserveEvent(prefix, ObserveEventNames.DowntimeStart, Set(obsIdParam, downtimeReasonParam))
  }

  private def createObserveEvent(eventName: EventName, obsId: ObsId) =
    ObserveEvent(prefix, eventName, Set(ParamFactories.obsIdParam(obsId)))

  private def createObserveEvent(eventName: EventName, exposureId: ExposureId) = {
    ObserveEvent(prefix, eventName, Set(ParamFactories.exposureIdParam(exposureId)))
  }

  private def createObserveEvent(eventName: EventName, exposureId: ExposureId, filename: String) = {
    ObserveEvent(prefix, eventName, Set(ParamFactories.exposureIdParam(exposureId), ParamFactories.filenameParam(filename)))
  }
}
