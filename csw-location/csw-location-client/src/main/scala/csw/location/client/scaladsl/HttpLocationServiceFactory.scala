/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.client.scaladsl

import org.apache.pekko.actor.typed.ActorSystem
import csw.location.api.CswVersionJvm
import csw.location.api.client.LocationServiceClient
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.{LocationRequest, LocationStreamRequest}
import csw.location.api.scaladsl.LocationService
import csw.location.client.internal.Settings
import msocket.api.ContentType.Json
import msocket.api.Transport
import msocket.http.post.HttpPostTransport
import msocket.http.ws.WebsocketTransport

/**
 * The factory is used to create LocationService instance.
 */
object HttpLocationServiceFactory extends LocationServiceCodecs {
  private val httpServerPort = Settings().serverPort

  /**
   * Use this factory method to create http location client when location server is running locally.
   * HTTP Location server runs on port 7654.
   */
  def makeLocalClient(implicit actorSystem: ActorSystem[?]): LocationService = make("localhost")

  /**
   * Use this factory method to create http location client when location server ip is known.
   * HTTP Location server runs on port 7654.
   */
  private[csw] def make(serverIp: String, port: Int, tokenFactory: () => Option[String] = () => None)(implicit
      actorSystem: ActorSystem[?]
  ): LocationService = {

    val httpUri      = s"http://$serverIp:$port/post-endpoint"
    val websocketUri = s"ws://$serverIp:$port/websocket-endpoint"
    val httpTransport: Transport[LocationRequest] =
      new HttpPostTransport[LocationRequest](httpUri, Json, tokenFactory)
    val websocketTransport: Transport[LocationStreamRequest] =
      new WebsocketTransport[LocationStreamRequest](websocketUri, Json)
    new LocationServiceClient(httpTransport, websocketTransport, new CswVersionJvm())
  }

  private[csw] def make(serverIp: String)(implicit actorSystem: ActorSystem[?]): LocationService =
    make(serverIp, httpServerPort)
}
