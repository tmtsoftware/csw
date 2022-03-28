/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.core.utils

import com.typesafe.config.Config

import scala.util.Try

object ConfigExt {
  implicit class RichConfig(private val underlying: Config) extends AnyVal {
    def getBooleanOrFalse(key: String): Boolean = Try { underlying.getBoolean(key) }.toOption.getOrElse(false)
  }
}
