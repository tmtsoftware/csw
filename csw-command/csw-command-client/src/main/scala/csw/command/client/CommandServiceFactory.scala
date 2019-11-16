package csw.command.client

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import csw.command.api.client.CommandServiceClient
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.internal.{CommandServiceImpl, JCommandServiceImpl}
import csw.command.client.messages.ComponentMessage
import csw.location.models.HttpLocation
import msocket.impl.Encoding.JsonText
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

/**
 * The factory helps in creating CommandService api for scala and java both
 */
trait ICommandServiceFactory {
  def make(componentLocation: HttpLocation)(implicit actorSystem: ActorSystem[_]): CommandService
  def jMake(componentLocation: HttpLocation, actorSystem: ActorSystem[_]): ICommandService
}

object CommandServiceFactory extends ICommandServiceFactory {

  /**
   * Make a CommandService instance for scala
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @param actorSystem of the component used for executing commands to other components and wait for the responses
   * @return an instance of type CommandService
   */
  def make(componentLocation: HttpLocation)(implicit actorSystem: ActorSystem[_]): CommandService = {
    import csw.command.api.codecs.CommandServiceCodecs._
    val baseUri      = componentLocation.uri.toString
    val webSocketUri = Uri(baseUri).withScheme("ws").withPath(Path("/websocket-endpoint"))
    val httpUri      = Uri(baseUri).withPath(Path("/post-endpoint"))
    println(httpUri)
    println(webSocketUri)
    new CommandServiceClient(
      new HttpPostTransport(httpUri.toString(), JsonText, () => None),
      new WebsocketTransport(webSocketUri.toString(), JsonText)
    )
  }

  def make2(component: ActorRef[ComponentMessage])(implicit actorSystem: ActorSystem[_]): CommandService =
    new CommandServiceImpl(component)

  /**
   * Make a CommandService instance for java
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @param actorSystem of the component used for executing commands to other components and wait for the responses
   * @return an instance of type ICommandService
   */
  def jMake(componentLocation: HttpLocation, actorSystem: ActorSystem[_]): ICommandService =
    new JCommandServiceImpl(make(componentLocation)(actorSystem))
}
