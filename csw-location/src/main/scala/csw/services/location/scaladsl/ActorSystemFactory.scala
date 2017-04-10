package csw.services.location.scaladsl

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.location.commons.{ClusterSettings, Constants}

/**
  * Creates a remote `ActorSystem` on the network interface where csw-cluster is running. The `ActorSystem` starts on
  * random port.
  *
  * @note It is highly recommended that if the `ActorRef` needs to be registered with `LocationService` then the `ActorSystem`
  *       in which it is created, should be created using `ActorSystemFactory` only
  */
object ActorSystemFactory {

  /**
    * Creates an `ActorSystem` with default name
    */
  def remote(): ActorSystem = remote(Constants.RemoteActorSystemName)

  /**
    * Creates an `ActorSystem` with the given name
    *
    * @note Even if the custom configuration is provided for the given `name`, it will be simply
    *       ignored, instead default remote configuration will be used while creating `ActorSystem`.
    */
  def remote(name: String): ActorSystem = {

    val config = ConfigFactory
      .parseString(s"akka.remote.netty.tcp.hostname = ${ClusterSettings().hostname}")
      .withFallback(ConfigFactory.load().getConfig(Constants.RemoteActorSystemName))

    ActorSystem(name, config)
  }
}


