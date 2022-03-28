/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services

import csw.config.server.http.HttpService
import csw.config.server.{ServerWiring, Main => ConfigMain}
import csw.services.internal.FutureExt._
import csw.services.internal.ManagedService

object ConfigServer {
  private val serviceName = "Config Service"
  private val initSvnRepo = "--initRepo"

  def configService(enable: Boolean, configPort: String): ManagedService[Option[(HttpService, ServerWiring)], Unit] =
    ManagedService[Option[(HttpService, ServerWiring)], Unit](
      serviceName,
      enable,
      () => start(configPort),
      stop
    )

  private def start(configPort: String): Option[(HttpService, ServerWiring)] =
    ConfigMain.start(Array("--port", configPort, initSvnRepo))

  private val stop: Option[(HttpService, ServerWiring)] => Unit = _.foreach { case (_, wiring) =>
    wiring.actorRuntime.shutdown().await()
  }
}
