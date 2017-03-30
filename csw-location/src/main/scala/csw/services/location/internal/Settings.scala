package csw.services.location.internal

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

case class Settings(values: Map[String, Any] = Map.empty) {
  def name: String = Constants.ClusterName

  val InterfaceNameKey = "interfaceName"
  val ClusterSeedKey = "clusterSeed"
  val IsSeedKey = "isSeed"

  def withEntry(key: String, value: Any): Settings = copy(values = values + (key → value))

  def withInterface(name: String): Settings = withEntry(InterfaceNameKey, name)

  def joinLocalSeed: Settings = withEntry(ClusterSeedKey, hostname)

  def asSeed: Settings = withEntry(IsSeedKey, "true")

  private lazy val allValues = values ++ sys.env ++ sys.props

  def hostname: String = {
    val interfaceName: String = allValues.getOrElse(InterfaceNameKey, "").toString
    new Networks(interfaceName).hostname()
  }

  def seedNodes: List[String] = (allValues.get(ClusterSeedKey), allValues.get(IsSeedKey)) match {
    case (Some(seed), _)   ⇒ List(s"akka.tcp://$name@$seed:3552")
    case (_, Some("true")) ⇒ List(s"akka.tcp://$name@$hostname:3552")
    case (_, _)            ⇒ List.empty
  }

  def port: Int = allValues.get(IsSeedKey) match {
    case Some("true") ⇒ 3552
    case _            ⇒ 0
  }

  def config: Config = {
    val computedValues: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" -> hostname,
      "akka.remote.netty.tcp.port" -> port,
      "akka.cluster.seed-nodes" -> seedNodes.asJava
    )

    ConfigFactory
      .parseMap(computedValues.asJava)
      .withFallback(ConfigFactory.load(name))
  }
}
