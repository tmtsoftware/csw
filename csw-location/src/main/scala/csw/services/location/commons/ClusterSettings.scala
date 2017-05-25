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
case class ClusterSettings(clusterName: String = Constants.ClusterName, values: Map[String, Any] = Map.empty)
    extends LocationServiceLogger.Simple {
  val InterfaceNameKey  = "interfaceName"
  val ClusterSeedsKey   = "clusterSeeds"
  val ClusterPortKey    = "clusterPort"
  val ManagementPortKey = "managementPort"

  def debug(): Unit =
    log.info(s"""
         |[debug] Using following cluster configurations:
         |[debug] ClusterSeedsKey: ${seedNodes.mkString(",")}
      """.stripMargin)

  private def withEntry(key: String, value: Any): ClusterSettings = copy(values = values + (key → value))
  def withEntries(entries: Map[String, Any]): ClusterSettings     = copy(values = values ++ entries)

  //InterfaceName should be ideally provided via env variables.
  //It should contain the value where csw-cluster has to be started.
  //This method should be used for testing only.
  def withInterface(name: String): ClusterSettings = withEntry(InterfaceNameKey, name)

  //ManagementPort should ideally be provided via system properties.
  //It is used to spawn akka cluster management service.
  //This method should be used for testing only.
  def withManagementPort(port: Int): ClusterSettings = withEntry(ManagementPortKey, port)

  def joinSeeds(seeds: String): ClusterSettings = withEntry(ClusterSeedsKey, seeds)

  //If no seeds are provided (which happens only during testing), then create a single node cluster by joining to self.
  def joinLocal(port: Int, ports: Int*): ClusterSettings = {
    val seeds = s"$hostname:$port" +: ports.map(port ⇒ s"$hostname:$port")
    joinSeeds(seeds.mkString(","))
  }

  //clusterPort should be ideally provided via env variables.
  def onPort(port: Int): ClusterSettings = withEntry(ClusterPortKey, port)

  //Config values for ActorSystem should be first picked from system variable then from environment properties and
  //then use the programmatically set variables.
  private lazy val allValues = sys.env ++ sys.props ++ values

  //If no interfaceName is provided then use empty value for it.
  private[location] def interfaceName: String = allValues.getOrElse(InterfaceNameKey, "").toString

  //Get the host address based on interfaceName provided.
  //If it is empty then get the default ipv4 address to start the current ActorSystem on.
  def hostname: String = new Networks(interfaceName).hostname()

  //Get the port for current ActorSystem to start. If no port is provided 0 will be used default.
  //SeedNode should start on a fixed port and rest all can start on random port.
  private[location] def port: Int = allValues.getOrElse(ClusterPortKey, 0).toString.toInt

  //Get the managementPort to start akka cluster management service.
  def managementPort: Option[Any] = allValues.get(ManagementPortKey)

  //Prepare a list of seedNodes provided via clusterSeeds
  def seedNodes: List[String] = {
    val seeds = allValues.get(ClusterSeedsKey).toList.flatMap(_.toString.split(",")).map(_.trim)
    seeds.map(seed ⇒ s"akka.tcp://$clusterName@$seed")
  }

  //Prepare config for ActorSystem to join csw-cluster
  def config: Config = {
    val computedValues: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname"        → hostname,
      "akka.remote.netty.tcp.port"            → port,
      "akka.cluster.seed-nodes"               → seedNodes.asJava,
      "akka.cluster.http.management.hostname" → hostname,
      "akka.cluster.http.management.port"     → managementPort.getOrElse(19999),
      "startManagement"                       → managementPort.isDefined
    )

    ConfigFactory
      .parseMap(computedValues.asJava)
      .withFallback(ConfigFactory.load().getConfig(clusterName))
  }
}

object ClusterAwareSettings extends ClusterSettings
