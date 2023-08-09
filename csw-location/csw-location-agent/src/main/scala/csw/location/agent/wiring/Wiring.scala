/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.agent.wiring
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[csw] class Wiring {
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "location-agent")
  val actorRuntime                                                  = new ActorRuntime(actorSystem)
  val locationService: LocationService                              = HttpLocationServiceFactory.makeLocalClient
}
