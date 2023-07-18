/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path
import csw.command.api.client.CommandServiceClient
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.command.client.internal.{CommandServiceImpl, JCommandServiceImpl}
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.{PekkoLocation, HttpLocation, Location, TcpLocation}
import msocket.api.ContentType.Json
import msocket.http.post.HttpPostTransport
import msocket.http.ws.WebsocketTransport

object CommandServiceFactory {

  private[csw] def make(component: ActorRef[ComponentMessage])(implicit actorSystem: ActorSystem[_]): CommandService =
    new CommandServiceImpl(component)

  /**
   * Make a CommandService instance for scala
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @param actorSystem of the component used for executing commands to other components and wait for the responses
   * @return an instance of type CommandService
   */
  def make(componentLocation: Location)(implicit actorSystem: ActorSystem[_]): CommandService = {
    componentLocation match {
      case _: TcpLocation => throw new RuntimeException("Only PekkoLocation and HttpLocation can be used to access a component")
      case pekkoLocation: PekkoLocation => new CommandServiceImpl(pekkoLocation.componentRef)
      case httpLocation: HttpLocation   => httpClient(httpLocation)
    }
  }

  /**
   * Make a CommandService instance for java
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @param actorSystem of the component used for executing commands to other components and wait for the responses
   * @return an instance of type ICommandService
   */
  def jMake(componentLocation: Location, actorSystem: ActorSystem[_]): ICommandService =
    new JCommandServiceImpl(make(componentLocation)(actorSystem))

  private def httpClient(httpLocation: HttpLocation)(implicit system: ActorSystem[_]) = {
    import csw.command.api.codecs.CommandServiceCodecs._
    val baseUri      = httpLocation.uri.toString
    val webSocketUri = Uri(baseUri).withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val httpUri      = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    new CommandServiceClient(
      new HttpPostTransport(httpUri, Json, () => None),
      new WebsocketTransport(webSocketUri, Json)
    )
  }
}
