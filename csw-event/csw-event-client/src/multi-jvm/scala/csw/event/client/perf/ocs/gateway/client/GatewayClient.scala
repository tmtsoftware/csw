/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.ocs.gateway.client

import org.apache.pekko.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path
import org.apache.pekko.stream.scaladsl.{Keep, Source}
import org.apache.pekko.stream.{KillSwitches, UniqueKillSwitch}
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import msocket.api.ContentType
import msocket.http.ws.WebsocketTransport
import org.scalatest.OptionValues.convertOptionToValuable

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class GatewayClient(subId: Int)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command]) extends GatewayCodecs {

  import GatewayMessages._
  import actorSystem.executionContext
  implicit val scheduler: Scheduler                 = actorSystem.scheduler
  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val gatewayPrefix                         = Prefix(ESW, "EswGateway")
  private val gatewayLocation                       = resolveHTTPLocation(gatewayPrefix, Service)
  private val serverIp                              = gatewayLocation.uri.getHost
  private val serverPort                            = gatewayLocation.uri.getPort

  def subscribe(keys: Set[EventKey]): Source[Event, UniqueKillSwitch] =
    gatewayWebSocketClient.requestStream[Event](Subscribe(keys)).viaMat(KillSwitches.single)(Keep.right)

  private def gatewayWebSocketClient = {
    val webSocketUri = Uri(s"http://$serverIp:$serverPort").withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val appName      = Some(s"Subscriber-$subId")
    new WebsocketTransport[GatewayStreamRequest](webSocketUri, ContentType.Json, () => None, appName)
  }


  private def resolveHTTPLocation(prefix: Prefix, componentType: ComponentType) = {
    val gatewayConnection = HttpConnection(ComponentId(prefix, componentType))
    Await.result(locationService.resolve(gatewayConnection, 3.seconds).map(_.value), 60.seconds)
  }
}
