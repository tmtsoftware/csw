/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract

import scala.io.Source
import scala.util.Using

object ResourceFetcher {
  def getResourceAsString(name: String): String = {
    Using.resource(Source.fromResource(name))(_.mkString)
  }
}
