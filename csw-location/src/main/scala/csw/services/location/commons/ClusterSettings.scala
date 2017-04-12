package csw.services.location.commons

import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.internal.Networks

import scala.collection.JavaConverters._

/**
  * ClusterSettings manages [[com.typesafe.config.Config]] values required by an [[akka.actor.ActorSystem]] to boot. It configures mainly
  * four parameters of an `ActorSystem`, namely :
  *
  *  - name (Name is defaulted to a constant value so that ActorSystem joins the cluster while booting)
  *  - akka.remote.netty.tcp.hostname (The hostname to boot an ActorSystem on)
  *  - akka.remote.netty.tcp.port     (The port to boot an ActorSystem on)
  *  - akka.cluster.seed-nodes        (Seed Nodes of the cluster)
  *
  * ClusterSettings require three values namely :
  *  - interfaceName (The network interface where cluster is formed.)
  *  - clusterSeeds (The host address of the seedNode of the cluster)
  *  - clusterPort (Specify port on which to start this service)
  *
  * The config values of the `ActorSystem` will be evaluated based on the above three settings as follows :
  *  - `akka.remote.netty.tcp.hostname` will be ipV4 address based on `interfaceName` from [[csw.services.location.internal.Networks]]
  *  - `akka.remote.netty.tcp.port` will be a random port or if `clusterPort` is specified that value will be picked
  *  - `akka.cluster.seed-nodes` will pick values of `clusterSeeds`
  *
  * If none of the settings are provided then defaults will be picked as follows :
  *  - `akka.remote.netty.tcp.hostname` will be ipV4 address from [[csw.services.location.internal.Networks]]
  *  - `akka.remote.netty.tcp.port` will be a random port
  *  - `akka.cluster.seed-nodes` will be empty
  * and an `ActorSystem` will be created and a cluster will be formed with no Seed Nodes. It will also self join the cluster.
  *
  * `ClusterSettings` can be given in three ways :
  *  - by using the api
  *  - by providing system properties
  *  - or by providing environment variables
  *
  * If a `ClusterSettings` value e.g. clusterPort is provided by more than one ways, then the precedence of consumption will be first from
  *  - System Properties
  *  - then from Environment variable
  *  - and then from `ClusterSettings` api
  *
  * ''Note : '' Although `ClusterSettings` can be added through multiple ways, it is recommended that
  *  -`clusterSeeds` is provided via environment variable,
  *  - `clusterPort` is provided via system properties,
  *  - `interfaceName` is provide via environment variable and
  *  - the `ClusterSettings` api of providing values should be used for testing purpose only.
  *
  */
case class ClusterSettings(clusterName: String = Constants.ClusterName, values: Map[String, Any] = Map.empty) {
  val InterfaceNameKey = "interfaceName"
  val ClusterSeedsKey = "clusterSeeds"
  val ClusterPortKey = "clusterPort"
  val ManagementPortKey = "managementPort"

  private def withEntry(key: String, value: Any): ClusterSettings = copy(values = values + (key → value))

  def withInterface(name: String): ClusterSettings = withEntry(InterfaceNameKey, name)

  def withManagementPort(port: Int): ClusterSettings = withEntry(ManagementPortKey, port)

  def joinSeeds(seed: String, seeds: String*): ClusterSettings = withEntry(ClusterSeedsKey, (seed +: seeds).mkString(","))

  def joinLocal(port: Int, ports: Int*): ClusterSettings = joinSeeds(s"$hostname:$port", ports.map(port ⇒ s"$hostname:$port"): _*)

  def onPort(port: Int): ClusterSettings = withEntry(ClusterPortKey, port)

  private lazy val allValues = values ++ sys.env ++ sys.props

  private[location] def interfaceName: String = allValues.getOrElse(InterfaceNameKey, "").toString

  def hostname: String = {
    new Networks(interfaceName).hostname()
  }

  private[location] def port: Int = allValues.getOrElse(ClusterPortKey, 0).toString.toInt

  def managementPort: Option[Any] = allValues.get(ManagementPortKey)

  private[location] def seedNodes: List[String] = {
    val seeds = allValues.get(ClusterSeedsKey).toList.flatMap(_.toString.split(",")).map(_.trim)
    seeds.map(seed ⇒ s"akka.tcp://$clusterName@$seed")
  }

  def config: Config = {
    val computedValues: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" → hostname,
      "akka.remote.netty.tcp.port" → port,
      "akka.cluster.seed-nodes" → seedNodes.asJava,
      "akka.cluster.http.management.hostname" → hostname,
      "akka.cluster.http.management.port" → managementPort.getOrElse(19999),
      "startManagement" → managementPort.isDefined
    )

    ConfigFactory
      .parseMap(computedValues.asJava)
      .withFallback(ConfigFactory.load().getConfig(clusterName))
  }
}

object ClusterAwareSettings extends ClusterSettings