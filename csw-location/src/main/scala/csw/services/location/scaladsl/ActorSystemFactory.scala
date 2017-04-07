package csw.services.location.scaladsl

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.location.commons.{ClusterSettings, Constants}

class ActorSystemFactory(name : String) {

  def this() = this(Constants.RemoteActorSystemName)

  def remote: ActorSystem = {

    val config = ConfigFactory
      .parseString(s"akka.remote.netty.tcp.hostname = ${ClusterSettings().hostname}")
      .withFallback(ConfigFactory.load().getConfig(name))

    ActorSystem(name, config)
  }
}


