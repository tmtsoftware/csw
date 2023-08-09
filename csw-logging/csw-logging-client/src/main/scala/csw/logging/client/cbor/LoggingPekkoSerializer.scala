/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.cbor

import csw.commons.CborPekkoSerializer
import csw.logging.models.codecs.LoggingCodecs.*
import csw.logging.models.codecs.LoggingSerializable
import csw.logging.models.{Level, LogMetadata}

class LoggingPekkoSerializer extends CborPekkoSerializer[LoggingSerializable] {
  override def identifier: Int = 19925

  register[LogMetadata]
  register[Level]
}
