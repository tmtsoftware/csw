package csw.params.events

import csw.params.core.models.ExposureId
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.DMS

/**
 *  DMS system notifies other systems by publishing these two observe events.
 */
object DMSObserveEvent {
  private lazy val sourcePrefix = Prefix(DMS, "Metadata")

  /**
   * This event indicates DMS has ingested the metadata following the exposureEnd.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def metadataAvailable(exposureId: ExposureId): ObserveEvent =
    create(ObserveEventNames.MetadataAvailable, exposureId)

  /**
   * This event  indicates that the raw science exposure has been stored and internal databases have been updated such that a client can request the exposure.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureAvailable(exposureId: ExposureId): ObserveEvent =
    create(ObserveEventNames.ExposureAvailable, exposureId)

  private def create(eventName: EventName, exposureId: ExposureId): ObserveEvent = {
    ObserveEvent(sourcePrefix, eventName, Set(ParamFactories.exposureIdParam(exposureId)))
  }
}
