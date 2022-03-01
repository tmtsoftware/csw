package csw.params.events

import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix

/**
 * The events that indicate activities for each observation and the acquisition process.
 * @param prefix [[csw.prefix.models.Prefix]] the prefix identifier of the sequencer which is generating this event.
 */
case class SequencerObserveEvent(prefix: Prefix) {

  /**
   * This event indicates the start of the preset phase of  acquisition
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def presetStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.PresetStart, obsId)

  /**
   * This event indicates the end of the preset phase of  acquisition
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def presetEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.PresetEnd, obsId)

  /**
   * This event indicates the start of locking the telescope to the  sky with guide and WFS targets
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def guidestarAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.GuidestarAcqStart, obsId)

  /**
   * This event indicates the end of locking the telescope to the sky with guide and WFS targets
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def guidestarAcqEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.GuidestarAcqEnd, obsId)

  /**
   * This event indicates the start of acquisition phase where  science target is peaked up as needed after  guidestar locking
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def scitargetAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ScitargetAcqStart, obsId)

  /**
   * This event indicates the end of acquisition phase where  science target is centered as needed after  guidestar locking
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def scitargetAcqEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ScitargetAcqEnd, obsId)

  /**
   * This event indicates the start of execution of actions related  to an observation including acquisition and  science data acquisition.
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observationStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObservationStart, obsId)

  /**
   * This event indicates the end of execution of actions related  to an observation including acquisition and  science data acquisition.
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observationEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObservationEnd, obsId)

  /**
   * This event indicates the start of execution of actions related  to an Observe command
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeStart(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObserveStart, obsId)

  /**
   * This event indicates the end of execution of actions related  to an Observe command
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeEnd(obsId: ObsId): ObserveEvent = createObserveEvent(ObserveEventNames.ObserveEnd, obsId)

  /**
   * This event indicates the start of data acquisition that  results in a file produced for DMS. This is a potential metadata event for DMS.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureStart(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ExposureStart, exposureId)

  /**
   * This event indicates the end of data acquisition that results  in a file produced for DMS. This is a potential metadata event for DMS.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureEnd(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ExposureEnd, exposureId)

  /**
   * This event indicates that a readout that is part of a ramp  has completed.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def readoutEnd(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ReadoutEnd, exposureId)

  /**
   * This event indicates that a readout that is part of a ramp  has failed indicating transfer failure or some  other issue.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def readoutFailed(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ReadoutFailed, exposureId)

  /**
   * This event indicates that the instrument has started writing  the exposure data file or transfer of exposure  data to DMS.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param filename   [[java.lang.String]] the path of the file.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def dataWriteStart(exposureId: ExposureId, filename: String): ObserveEvent =
    createObserveEvent(ObserveEventNames.DataWriteStart, exposureId, filename)

  /**
   * This event indicates that the instrument has finished  writing the exposure data file or transfer of  exposure data to DMS.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param filename   [[java.lang.String]] the path of the file.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def dataWriteEnd(exposureId: ExposureId, filename: String): ObserveEvent =
    createObserveEvent(ObserveEventNames.DataWriteEnd, exposureId, filename)

  /**
   * This event indicates that the detector system is preparing to start an exposure.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def prepareStart(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.PrepareStart, exposureId)

  /**
   * This event indicates that a request was made to abort the  exposure and it has completed. Normal data events should occur if data is  recoverable.
   * Abort should not fail
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureAborted(exposureId: ExposureId): ObserveEvent =
    createObserveEvent(ObserveEventNames.ExposureAborted, exposureId)

  /**
   * This event indicates that a user has paused the current  observation Sequence which will happen after  the current step concludes
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observePaused(): ObserveEvent = ObserveEvent(prefix, ObserveEventNames.ObservePaused)

  /**
   * This event indicates that a user has resumed a paused  observation Sequence.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def observeResumed(): ObserveEvent = ObserveEvent(prefix, ObserveEventNames.ObserveResumed)

  /**
   * This event indicates that something has occurred that  interrupts the normal observing workflow and  time accounting.
   * This event will have a hint (TBD) that indicates  the cause of the downtime for statistics.
   * Examples are: weather, equipment or other  technical failure, etc.
   * Downtime is ended by the start of an observation  or exposure.
   * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
   * @param reasonForDowntime [[java.lang.String]] a hint that indicates the cause of the downtime for statistics.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = ParamFactories.obsIdParam(obsId)
    val downtimeReasonParam = ParamFactories.downTimeReasonParam(reasonForDowntime)
    ObserveEvent(prefix, ObserveEventNames.DowntimeStart, Set(obsIdParam, downtimeReasonParam))
  }

  /**
   * This event indicates the start of a telescope offset or dither
   * @param obsId [[csw.params.core.models.ObsId]] representing a unique observation id
   * @param p [[java.lang.Double]] Represents telescope's xCoordinate offset
   * @param q [[java.lang.Double]] Represents telescope's yCoordinate offset
   * @return [[csw.params.events.ObserveEvent]]
   */
  def offsetStart(obsId: ObsId, p: Double, q: Double): ObserveEvent =
    ObserveEvent(
      prefix,
      ObserveEventNames.OffsetStart,
      Set(ParamFactories.obsIdParam(obsId), ParamFactories.pOffsetParam(p), ParamFactories.qOffsetParam(q))
    )

  /**
   * This event indicates the end of a telescope offset or dither
   * @param obsId [[csw.params.core.models.ObsId]] representing a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def offsetEnd(obsId: ObsId): ObserveEvent =
    ObserveEvent(prefix, ObserveEventNames.OffsetEnd, Set(ParamFactories.obsIdParam(obsId)))

  /**
   * This event indicates the start of a request to the user for input
   * @param obsId [[csw.params.core.models.ObsId]] Representing a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def inputRequestStart(obsId: ObsId): ObserveEvent = {
    ObserveEvent(prefix, ObserveEventNames.InputRequestStart, Set(ParamFactories.obsIdParam(obsId)))
  }

  /**
   * This event indicates the end of a request to the user for input
   * @param obsId [[csw.params.core.models.ObsId]] Representing a unique observation id
   * @return [[csw.params.events.ObserveEvent]]
   */
  def inputRequestEnd(obsId: ObsId): ObserveEvent = {
    ObserveEvent(prefix, ObserveEventNames.InputRequestEnd, Set(ParamFactories.obsIdParam(obsId)))
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
