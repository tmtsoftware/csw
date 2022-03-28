/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.javadsl

import csw.logging.models.RequestId

/**
 * Helper class for Java to get the handle of RequestId
 */
object JRequestId {
  val id = RequestId()
}
