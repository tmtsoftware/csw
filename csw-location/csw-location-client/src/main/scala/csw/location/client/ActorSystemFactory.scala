package csw.location.client

import akka.actor.typed.{Behavior, ActorSystem => TypedActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.commons.{Constants, LocationServiceLogger}
import csw.logging.api.scaladsl.Logger
import csw.network.utils.Networks

/**
 * ActorSystemFactory creates a remote ActorSystem on the interface where csw-cluster is running. The ActorSystem starts on a
 * random port.
 *
 * @note it is recommended to create actors via this factory if it has to be registered with LocationService
 */
object ActorSystemFactory {

  private val log: Logger = LocationServiceLogger.getLogger

  private lazy val config = ConfigFactory
    .parseString(s"akka.remote.artery.canonical.hostname = ${Networks().hostname}")
    .withFallback(ConfigFactory.load().getConfig(Constants.RemoteActorSystemName))
    .withFallback(ConfigFactory.defaultApplication().resolve())

  /**
   * Create an Typed ActorSystem with the given guardian behaviour, name and remote properties
   *
   * @note even if the custom configuration is provided for the given name of ActorSystem, it will be simply
   *       ignored, instead default remote configuration will be used while creating ActorSystem
   */
  def remote[T](behavior: Behavior[T], name: String): TypedActorSystem[T] = {
    log.info(s"Creating remote actor system with name $name")
    TypedActorSystem(behavior, name, config)
  }

  def remote[T](behavior: Behavior[T], name: String, _config: Config): TypedActorSystem[T] = {
    log.info(s"Creating remote actor system with name $name")
    TypedActorSystem(behavior, name, _config.withFallback(config))
  }

  /**
   * Create an ActorSystem with `Constants.RemoteActorSystemName` as componentName
   */
  def remote[T](behavior: Behavior[T]): TypedActorSystem[T] = {
    remote(behavior, Constants.RemoteActorSystemName)
  }
}
