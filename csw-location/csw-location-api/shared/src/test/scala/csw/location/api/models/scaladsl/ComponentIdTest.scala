/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.models.scaladsl

import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ComponentIdTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // DEOPSCSW-14: Codec for data model
  test("should not contain leading or trailing spaces in component's name | DEOPSCSW-14") {

    val illegalArgumentException = intercept[IllegalArgumentException] {
      ComponentId(Prefix(Subsystem.CSW, " redis "), ComponentType.Service)
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has leading and trailing whitespaces"
  }

  // DEOPSCSW-14: Codec for data model
  test("should not contain '-' in component's name | DEOPSCSW-14") {
    val illegalArgumentException = intercept[IllegalArgumentException] {
      ComponentId(Prefix(Subsystem.CSW, "redis-service"), ComponentType.Service)
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has '-'"
  }
}
