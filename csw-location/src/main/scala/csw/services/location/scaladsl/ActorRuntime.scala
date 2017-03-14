package csw.services.location.scaladsl

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.common.Networks

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class ActorRuntime(name: String, _settings: Map[String, Any] = Map.empty) {

  val config: Config = {
    val settings: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" -> Networks.getPrimaryIpv4Address.getHostAddress
    ) ++ _settings

    ConfigFactory.parseMap(settings.asJava).withFallback(ConfigFactory.load())
  }

  implicit val actorSystem: ActorSystem = ActorSystem(name, config)
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()

  def makeMat(): Materializer = ActorMaterializer()
}
