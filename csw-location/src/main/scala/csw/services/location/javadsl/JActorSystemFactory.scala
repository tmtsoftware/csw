package csw.services.location.javadsl

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.scaladsl.ActorSystemFactory

object JActorSystemFactory {

    private val defaultConfig = ConfigFactory.load().getConfig("csw-actor-system")

    def create(): ActorSystem = new ActorSystemFactory("csw-actor-system", defaultConfig).make

    def create(name : String): ActorSystem = new ActorSystemFactory(name, defaultConfig).make

    def create(name: String, config: Config): ActorSystem = new ActorSystemFactory("csw-actor-system", config).make
}
