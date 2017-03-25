package csw.services.location.internal

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

case class Settings(values: Map[String, Any] = Map.empty) {
  def withEntry(key: String, value: Any): Settings = Settings(values + (key → value))

  def withInterface(name: String): Settings = withEntry("interfaceName", name)

  def withPort(port: Int): Settings = withEntry("akka.remote.netty.tcp.port", port)
}

case class ConfigData(settings: Map[String, Any]) {
  private val interfaceName = settings.getOrElse("interfaceName", "").toString

  private val hostname: String = Networks.getIpv4Address(interfaceName).getHostAddress

  private val port = sys.props.getOrElse("akkaPort", "2552")
  private val seedHost = sys.props.getOrElse("akkaSeed", hostname)
  private def seedNode(name: String) = s"akka.tcp://$name@$seedHost:2552"

  def config(name: String): Config = {
    val allSettings: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" -> hostname,
      "akka.remote.netty.tcp.port" -> port,
      "akka.cluster.seed-nodes" -> List(seedNode(name)).asJava
    ) ++ settings

    ConfigFactory.parseMap(allSettings.asJava).withFallback(ConfigFactory.load())
  }
}
