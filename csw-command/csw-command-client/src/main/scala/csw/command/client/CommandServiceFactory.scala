package csw.command.client

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import csw.command.api.client.CommandServiceClient
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.internal.{CommandServiceImpl, JCommandServiceImpl}
import csw.command.client.messages.ComponentMessage
import csw.location.models.{AkkaLocation, HttpLocation, Location, TcpLocation}
import msocket.api.Encoding.JsonText
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

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
      case _: TcpLocation             => throw new RuntimeException("Only AkkaLocation and HttpLocation can be used to access a component")
      case akkaLocation: AkkaLocation => new CommandServiceImpl(akkaLocation.componentRef)
      case httpLocation: HttpLocation => httpClient(httpLocation)
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
      new HttpPostTransport(httpUri, JsonText, () => None),
      new WebsocketTransport(webSocketUri, JsonText)
    )
  }
}
