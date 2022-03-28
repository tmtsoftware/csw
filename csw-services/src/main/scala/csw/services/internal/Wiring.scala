/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services.internal

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http.ServerBinding
import csw.config.server.http.HttpService
import csw.config.server.{ServerWiring => ConfigWiring}
import csw.location.agent.wiring.{Wiring => AgentWiring}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.internal.{ServerWiring => LocationWiring}
import csw.services._
import csw.services.cli.Command.Start
import csw.services.utils.ColoredConsole
import org.tmt.embedded_keycloak.impl.StopHandle

import scala.concurrent.ExecutionContext

class Wiring(startCmd: Start) {
  import startCmd._
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  lazy implicit val ec: ExecutionContext                            = actorSystem.executionContext

  lazy val settings: Settings                     = Settings(startCmd.interfaceName, startCmd.outsideInterfaceName)
  lazy val locationServiceClient: LocationService = HttpLocationServiceFactory.makeLocalClient

  lazy val environment   = new Environment(settings)
  lazy val locationAgent = new LocationAgent(settings)
  lazy val redis         = new Redis(settings)
  lazy val keycloak      = new AuthServer(locationServiceClient, settings)

  lazy val locationService: ManagedService[Option[(ServerBinding, LocationWiring)], Unit] =
    LocationServer.locationService(enable = true, settings.clusterPort)
  lazy val eventService: ManagedService[Process, Unit]                           = redis.eventService(event)
  lazy val alarmService: ManagedService[Process, Unit]                           = redis.alarmService(alarm)
  lazy val sentinelService: ManagedService[Option[(Process, AgentWiring)], Unit] = locationAgent.sentinelService(event, alarm)
  lazy val databaseService: ManagedService[Option[(Process, AgentWiring)], Unit] = locationAgent.databaseService(database)
  lazy val aasService: ManagedService[StopHandle, Unit]                          = keycloak.aasService(config || auth)
  lazy val configService: ManagedService[Option[(HttpService, ConfigWiring)], Unit] =
    ConfigServer.configService(config, settings.configPort)

  lazy val serviceList = List(
    locationService,
    databaseService,
    eventService,
    alarmService,
    sentinelService,
    aasService,
    configService
  )

  def shutdown(): Unit = {
    ColoredConsole.GREEN.println("Shutdown started ...")
    serviceList.reverse.foreach(_.stop)
    ColoredConsole.GREEN.println("Shutdown finished!")
  }
}
