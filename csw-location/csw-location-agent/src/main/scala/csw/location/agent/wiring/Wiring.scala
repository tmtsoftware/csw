/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.agent.wiring
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[csw] class Wiring {
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "location-agent")
  lazy val actorRuntime                                             = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
}
