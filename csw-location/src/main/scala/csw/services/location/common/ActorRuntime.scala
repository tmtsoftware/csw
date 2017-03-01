package csw.services.location.common

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object ActorRuntime {

  private def config = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.hostname=${Networks.getPrimaryIpv4Address.getHostAddress}")
    .withFallback(ConfigFactory.load())

  def make(name: String): ActorSystem =  ActorSystem(name, config)
}
