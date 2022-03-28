/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.commons

import scala.util.Random

object RandomUtils {
  def randomFrom[T](values: Iterable[T]): T = values.toList(Random.nextInt(values.size))
  def randomString(size: Int): String       = Random.alphanumeric.take(size).mkString
  def randomString5(): String               = randomString(5)
}
