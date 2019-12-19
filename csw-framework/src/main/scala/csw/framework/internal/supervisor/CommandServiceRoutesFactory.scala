package csw.framework.internal.supervisor

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.client.CommandServiceFactory
import csw.command.client.handlers.{CommandServiceHttpHandlers, CommandServiceWebsocketHandlers}
import csw.command.client.messages.ComponentMessage
import msocket.api.Encoding
import msocket.impl.RouteFactory
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory

object CommandServiceRoutesFactory {

  import CommandServiceCodecs._

  def createRoutes(component: ActorRef[ComponentMessage])(implicit actorSystem: ActorSystem[_]): Route = {
    val commandService                                  = CommandServiceFactory.make(component)
    val httpHandlers                                    = new CommandServiceHttpHandlers(commandService)
    def websocketHandlersFactory(encoding: Encoding[_]) = new CommandServiceWebsocketHandlers(commandService, encoding)
    RouteFactory.combine(
      new PostRouteFactory("post-endpoint", httpHandlers),
      new WebsocketRouteFactory("websocket-endpoint", websocketHandlersFactory)
    )
  }

}
