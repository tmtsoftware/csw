/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.formats

import io.bullet.borer.Input

object JInput {
  val FromByteArrayProvider: Input.Provider[Array[Byte]] = Input.FromByteArrayProvider
}
