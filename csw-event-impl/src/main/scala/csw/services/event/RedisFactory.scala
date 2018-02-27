package csw.services.event

import akka.actor.ActorSystem
import csw.services.event.internal.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}

object RedisFactory {

  def publisher(host: String, port: Int)(implicit _actorSystem: ActorSystem): EventPublisher = {
    new Wiring(host, redisPort = port) {
      override implicit lazy val actorSystem: ActorSystem = _actorSystem
    }.redisPublisher
  }

  def subscriber(host: String, port: Int)(implicit _actorSystem: ActorSystem): EventSubscriber = {
    new Wiring(host, redisPort = port) {
      override implicit lazy val actorSystem: ActorSystem = _actorSystem
    }.redisSubscriber
  }
}
