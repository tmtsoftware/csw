/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.location.server.internal.ServerWiring
import csw.location.server.{Main => LocationMain}
import csw.services.internal.FutureExt._
import csw.services.internal.ManagedService

object LocationServer {
  private val serviceName = "Location Service"

  def locationService(enable: Boolean, clusterPort: String): ManagedService[Option[(ServerBinding, ServerWiring)], Unit] =
    ManagedService[Option[(ServerBinding, ServerWiring)], Unit](
      serviceName,
      enable,
      () => start(clusterPort),
      stop
    )

  private def start(clusterPort: String): Option[(Http.ServerBinding, ServerWiring)] =
    LocationMain.start(Array("--clusterPort", clusterPort))

  private val stop: Option[(Http.ServerBinding, ServerWiring)] => Unit = _.foreach { case (_, wiring) =>
    wiring.actorRuntime.shutdown().await()
  }

}
