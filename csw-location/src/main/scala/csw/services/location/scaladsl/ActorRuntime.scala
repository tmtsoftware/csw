package csw.services.location.scaladsl

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.impl.Networks

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

class ActorRuntime(name: String, _settings: Map[String, Any], interfaceName: String = "") {
  def this(name: String) = this(name, Map.empty, "")
  def this(name: String,_settings: Map[String, Any]) = this(name, _settings, "")
  def this(name: String, interfaceName: String ) = this(name, Map.empty, interfaceName)

  val ipaddr: InetAddress = Networks.getIpv4Address(interfaceName)

  val config: Config = {
    val settings: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" -> ipaddr.getHostAddress
    ) ++ _settings

    ConfigFactory.parseMap(settings.asJava).withFallback(ConfigFactory.load())
  }

  implicit val actorSystem: ActorSystem = ActorSystem(name, config)
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()
  implicit val timeout: Timeout = Timeout(2.seconds)

  def makeMat(): Materializer = ActorMaterializer()

  val jmDnsDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup("jmdns.dispatcher")

}
