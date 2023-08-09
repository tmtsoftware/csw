/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.auth.installed

import java.nio.file.Paths

import org.apache.pekko.actor.typed
import csw.aas.installed.InstalledAppAuthAdapterFactory
import csw.aas.installed.api.InstalledAppAuthAdapter
import csw.aas.installed.scaladsl.FileAuthStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.ExecutionContextExecutor

// #adapter-factory
object AdapterFactory {
  def makeAdapter(implicit actorSystem: typed.ActorSystem[_]): InstalledAppAuthAdapter = {
    implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
    val locationService: LocationService      = HttpLocationServiceFactory.makeLocalClient(actorSystem)
    val authStore                             = new FileAuthStore(Paths.get("/tmp/demo-cli/auth"))
    InstalledAppAuthAdapterFactory.make(locationService, authStore)
  }
}
// #adapter-factory
