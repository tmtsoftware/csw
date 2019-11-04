package csw.location.server.http

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.scaladsl.Source
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.server.internal.ActorRuntime
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.post.ServerHttpCodecs
import msocket.impl.ws.WsServerFlow

private[csw] class LocationRoutes(
    httpHandler: MessageHandler[LocationHttpMessage, StandardRoute],
    websocketHandler: Encoding[_] => MessageHandler[LocationWebsocketMessage, Source[Message, NotUsed]],
    locationExceptionHandler: LocationExceptionHandler,
    actorRuntime: ActorRuntime
) extends ServerHttpCodecs
    with LocationServiceCodecs {

  import actorRuntime._

  val routes: Route = cors() {
    get {
      path("websocket-endpoint") {
        handleWebSocketMessages {
          new WsServerFlow(websocketHandler).flow
        }
      }
    } ~
    post {
      path("post-endpoint") {
        entity(as[LocationHttpMessage])(httpHandler.handle)
      }
    }
  }
}
