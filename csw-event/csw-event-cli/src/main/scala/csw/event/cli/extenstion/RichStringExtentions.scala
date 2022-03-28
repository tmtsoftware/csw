/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli.extenstion

import java.nio.charset.StandardCharsets

import io.bullet.borer
import io.bullet.borer.Codec

object RichStringExtentions {
  implicit class JsonDecodeRichString(val value: String) extends AnyVal {
    def parse[T: Codec]: T = borer.Json.decode(value.getBytes(StandardCharsets.UTF_8)).to[T].value
  }
}
