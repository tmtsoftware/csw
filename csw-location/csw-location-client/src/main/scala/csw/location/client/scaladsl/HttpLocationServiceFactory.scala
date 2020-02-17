package csw.location.client.scaladsl

import akka.actor.typed.ActorSystem
import csw.location.api.client.LocationServiceClient
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.scaladsl.LocationService
import csw.location.client.internal.Settings
import msocket.api.ContentType.Json
import msocket.api.Transport
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

/**
 * The factory is used to create LocationService instance.
 */
object HttpLocationServiceFactory extends LocationServiceCodecs {

  private val httpServerPort = Settings().serverPort

  /**
   * Use this factory method to create http location client when location server is running locally.
   * HTTP Location server runs on port 7654.
   * */
  def makeLocalClient(implicit actorSystem: ActorSystem[_]): LocationService = make("localhost")

  /**
   * Use this factory method to create http location client when location server ip is known.
   * HTTP Location server runs on port 7654.
   * */
  private[csw] def make(serverIp: String, port: Int, tokenFactory: () => Option[String] = () => None)(
      implicit actorSystem: ActorSystem[_]
  ): LocationService = {
    val httpUri      = s"http://$serverIp:$port/post-endpoint"
    val websocketUri = s"ws://$serverIp:$port/websocket-endpoint"
    val httpTransport: Transport[LocationHttpMessage] =
      new HttpPostTransport[LocationHttpMessage](httpUri, Json, tokenFactory)
    val websocketTransport: Transport[LocationWebsocketMessage] =
      new WebsocketTransport[LocationWebsocketMessage](websocketUri, Json)
    new LocationServiceClient(httpTransport, websocketTransport)
  }

  private[csw] def make(serverIp: String)(implicit actorSystem: ActorSystem[_]): LocationService =
    make(serverIp, httpServerPort)

}
