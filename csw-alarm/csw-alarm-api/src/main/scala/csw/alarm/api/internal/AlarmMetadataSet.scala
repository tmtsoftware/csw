/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.api.internal

import csw.alarm.models.AlarmMetadata

private[alarm] case class AlarmMetadataSet(alarms: Set[AlarmMetadata])
