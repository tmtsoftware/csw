package csw.services.location.scaladsl

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.commons.ClusterSettings

final case class ActorSystemFactory(name : String, config: Config) {

  def make: ActorSystem = {
    val fallback = ConfigFactory
      .parseString(s"akka.remote.netty.tcp.hostname = ${ClusterSettings().hostname}")
      .withFallback(config)

    ActorSystem(name, fallback)
  }
}

object ActorSystemFactory {
  private val defaultConfig = ConfigFactory.load().getConfig("csw-actor-system")

  def apply(): ActorSystemFactory = new ActorSystemFactory("csw-actor-system", defaultConfig)

  def apply(name : String): ActorSystemFactory = new ActorSystemFactory(name, defaultConfig)
}


