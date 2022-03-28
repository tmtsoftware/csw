/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.integtration.apps

import csw.integtration.tests.LocationServiceMultipleNICTest
import org.scalatest

object TestMultipleNicApp {
  def main(args: Array[String]): Unit = {
    scalatest.run(new LocationServiceMultipleNICTest())
  }
}
