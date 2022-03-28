/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.cbor

import csw.command.client.models.framework.LocationServiceUsage
import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterAndTrackServices, RegisterOnly}
import io.bullet.borer.Cbor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

class CborTest extends AnyFunSuite with Matchers with MessageCodecs {
  test("should encode concrete-type LocationServiceUsage and decode base-type") {
    val testData = Table(
      "LocationServiceUsage models",
      DoNotRegister,
      RegisterOnly,
      RegisterAndTrackServices
    )

    forAll(testData) { locationServiceUsage =>
      val bytes = Cbor.encode[LocationServiceUsage](locationServiceUsage).toByteArray
      Cbor.decode(bytes).to[LocationServiceUsage].value shouldEqual locationServiceUsage
    }
  }
}
