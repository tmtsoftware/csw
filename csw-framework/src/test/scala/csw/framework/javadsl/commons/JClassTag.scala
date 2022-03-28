/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.javadsl.commons

import scala.reflect.ClassTag

object JClassTag {
  def make[T](klass: Class[T]): ClassTag[T] = ClassTag(klass)
}
