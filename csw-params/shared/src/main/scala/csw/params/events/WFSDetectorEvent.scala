/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.events

import csw.params.core.generics.Parameter
import csw.params.core.models.ExposureId
import csw.prefix.models.Prefix

/**
 * Wavefront detector system Observe events
 */
object WFSDetectorEvent {
  private def create(sourcePrefix: Prefix, eventName: EventName): ObserveEvent = ObserveEvent(sourcePrefix, eventName)

  /**
   * This event indicates the WFS or guider detector system has  successfully published an image to VBDS.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def publishSuccess(sourcePrefix: Prefix): ObserveEvent = create(sourcePrefix, ObserveEventNames.PublishSuccess)

  /**
   * This event indicates that a WFS or guider detector system  has failed while publishing an image to VBDS.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def publishFail(sourcePrefix: Prefix): ObserveEvent = create(sourcePrefix, ObserveEventNames.PublishFail)

  /**
   * A state  variable to indicate the current state of the detector system.
   * The Exposure State Event groups  parameters that change relatively slowly, and
   * this event should be published whenever any of its  parameters changes.
   * @param sourcePrefix [[csw.prefix.models.Prefix]] the prefix identifier of the source which is generating this event.
   * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
   *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
   *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
   *                   when the ExposureId is created.
   * @param exposureInProgress [[java.lang.Boolean]] indicating if detector system is acquiring an exposure.
   *                           Delimited by exposureStart and exposureEnd. exposureInProgress should be false if abortInProgress is true (TBD)
   * @param abortInProgress [[java.lang.Boolean]] indicates that an abort has been requested and is underway.
   * @param isAborted [[java.lang.Boolean]] Indicates that an abort has occurred and is completed. abortInProgress should be false when isAborted is true.
   *                  isAborted should be set to false with the next exposure
   * @param errorMessage [[java.lang.String]] An parameter that can be included when the detector system  is in the ERROR operationalState.
   *                     This value should be cleared and removed from the state when the  operationalState returns to READY
   * @param operationalState [[csw.params.events.OperationalState]] indicating if the detector system is available and  operational.
   * @return [[csw.params.events.ObserveEvent]]
   */
  def exposureState(
      sourcePrefix: Prefix,
      exposureId: ExposureId,
      exposureInProgress: Boolean,
      abortInProgress: Boolean,
      isAborted: Boolean,
      operationalState: OperationalState,
      errorMessage: String
  ): ObserveEvent = {
    val params: Set[Parameter[?]] = Set(
      ParamFactories.exposureIdParam(exposureId),
      ParamFactories.operationalStateParam(operationalState),
      ParamFactories.errorMessageParam(errorMessage),
      ParamFactories.exposureInProgressParam(exposureInProgress),
      ParamFactories.abortInProgressParam(abortInProgress),
      ParamFactories.isAbortedParam(isAborted)
    )
    ObserveEvent(sourcePrefix, ObserveEventNames.WfsDetectorExposureState, params)
  }
}
