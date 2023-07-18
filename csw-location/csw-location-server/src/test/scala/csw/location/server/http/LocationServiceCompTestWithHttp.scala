/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import csw.location.server.scaladsl.LocationServiceCompTest

import scala.concurrent.duration.DurationInt

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {
  private var wiring: ServerWiring = _

  override protected def beforeAll(): Unit = {
    wiring = new ServerWiring(false)
    val locationBinding = wiring.locationHttpService.start().await
    wiring.actorRuntime.coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseServiceUnbind,
      "unbind-services"
    )(() => locationBinding.terminate(30.seconds).map(_ => Done)(wiring.actorRuntime.ec))
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    wiring.actorRuntime.shutdown().await
    super.afterAll()
  }
}
