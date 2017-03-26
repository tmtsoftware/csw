package csw.services.location.internal

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

case class Settings(name: String, values: Map[String, Any] = Map.empty) {
  def withEntry(key: String, value: Any): Settings = copy(values = values + (key → value))

  def withInterface(name: String): Settings = withEntry("interfaceName", name)

  def withPort(port: Int): Settings = withEntry("akka.remote.netty.tcp.port", port)

  def config: Config = {
    val interfaceName = values.getOrElse("interfaceName", "").toString
    val hostname: String = Networks.getIpv4Address(interfaceName).getHostAddress

    val port = sys.props.getOrElse("akkaPort", "2552")
    val seedHost = sys.props.getOrElse("akkaSeed", hostname)
    val seedNode = s"akka.tcp://$name@$seedHost:2552"

    val computedValues = Map(
      "akka.remote.netty.tcp.hostname" -> hostname,
      "akka.remote.netty.tcp.port" -> port,
      "akka.cluster.seed-nodes" -> List(seedNode).asJava
    )

    val allValues: Map[String, Any] = computedValues ++ values

    ConfigFactory.parseMap(allValues.asJava).withFallback(ConfigFactory.load())
  }
}
