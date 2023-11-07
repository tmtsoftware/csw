/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.integtration.apps

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import csw.integtration.common.TestFutureExtension.given
import scala.language.implicitConversions

import csw.location.api.PekkoRegistrationFactory
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.PekkoConnection
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.internal.ServerWiring
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS

object AssemblyApp {
  private val locationWiring = ServerWiring.make(ClusterAwareSettings.onPort(3553).withInterface("eth1"), enableAuth = false)
  locationWiring.actorRuntime.startLogging("Assembly", locationWiring.clusterSettings.hostname)
  locationWiring.locationHttpService.start().await

  import locationWiring.actorRuntime._

  private val assemblyActorRef = actorSystem.spawn(behavior, "assembly")
  private val componentId      = ComponentId(Prefix(NFIRAOS, "assembly"), Assembly)
  private val connection       = PekkoConnection(componentId)

  private val registration       = PekkoRegistrationFactory.make(connection, assemblyActorRef)
  private val locationService    = HttpLocationServiceFactory.makeLocalClient
  private val registrationResult = locationService.register(registration).await

  def behavior: Behaviors.Receive[String] =
    Behaviors.receiveMessagePartial[String] { case "Unregister" =>
      registrationResult.unregister()
      Behaviors.same
    }

  def main(args: Array[String]): Unit = {}
}
