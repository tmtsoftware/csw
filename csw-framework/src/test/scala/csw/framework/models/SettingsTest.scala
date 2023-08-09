/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.models

import org.apache.pekko.actor.typed.SpawnProtocol
import csw.event.client.helpers.TestFutureExt.given
import scala.language.implicitConversions

import csw.location.client.ActorSystemFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SettingsTest extends AnyFunSuite with Matchers {
  test("settings should be able to read default values for crm config | CSW-160") {
    val actorSystem = ActorSystemFactory.remote(SpawnProtocol(), "crm-config-test")

    val settings = new Settings(actorSystem)

    settings.startedSize shouldBe 50
    settings.responseSize shouldBe 50
    settings.waiterSize shouldBe 50

    actorSystem.terminate()
    actorSystem.whenTerminated.await
  }
}
