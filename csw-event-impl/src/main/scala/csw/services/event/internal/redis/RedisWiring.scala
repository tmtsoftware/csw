package csw.services.event.internal.redis

import java.net.URI

import akka.actor.ActorSystem
import csw.services.event.internal.commons.Wiring
import io.lettuce.core.RedisURI

class RedisWiring(host: String, port: Int, actorSystem: ActorSystem) extends Wiring(host, port, actorSystem) {

  lazy val redisURI: RedisURI = RedisURI.create(host, port)
  lazy val redisGateway       = new RedisGateway(redisURI)

  override def publisher(): RedisPublisher   = new RedisPublisher(redisGateway)
  override def subscriber(): RedisSubscriber = new RedisSubscriber(redisGateway)
}

object RedisWiring {
  def apply(uri: URI, actorSystem: ActorSystem): RedisWiring = new RedisWiring(uri.getHost, uri.getPort, actorSystem)
}
