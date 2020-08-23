package csw.framework.internal.supervisor

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.client.CommandServiceFactory
import csw.command.client.handlers.{CommandServiceRequestHandler, CommandServiceStreamingRequestHandler}
import csw.command.client.messages.ComponentMessage
import msocket.impl.RouteFactory
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory

object CommandServiceRoutesFactory {

  import CommandServiceCodecs._

  def createRoutes(component: ActorRef[ComponentMessage])(implicit actorSystem: ActorSystem[_]): Route = {
    val commandService     = CommandServiceFactory.make(component)
    val securityDirectives = SecurityDirectives.authDisabled(actorSystem.settings.config)(actorSystem.executionContext)
    val httpHandlers       = new CommandServiceRequestHandler(commandService, securityDirectives)
    val websocketHandlers  = new CommandServiceStreamingRequestHandler(commandService)
    RouteFactory.combine(metricsEnabled = false)(
      new PostRouteFactory("post-endpoint", httpHandlers),
      new WebsocketRouteFactory("websocket-endpoint", websocketHandlers)
    )
  }

}
