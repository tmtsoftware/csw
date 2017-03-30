package csw.services.location.internal

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

/**
  * Settings manages [[com.typesafe.config.Config]] values required by an [[akka.actor.ActorSystem]] to boot. It configures mainly
  * four parameters of an `ActorSystem`, namely :
  *
  *  - name (Name is defaulted to a constant value so that ActorSystem joins the cluster while booting)
  *  - akka.remote.netty.tcp.hostname (The hostname to boot an ActorSystem on)
  *  - akka.remote.netty.tcp.port     (The port to boot an ActorSystem on)
  *  - akka.cluster.seed-nodes        (Seed Nodes of the cluster)
  *
  * Settings require three values namely :
  *  - interfaceName (The network interface where cluster is formed.)
  *  - clusterSeed (The host address of the seedNode of the cluster)
  *  - isSeed (Claim self to be the seed of the cluster)
  *
  * The config values of the `ActorSystem` will be evaluated based on the above three settings as follows :
  *  - `akka.remote.netty.tcp.hostname` will be ipV4 address based on `interfaceName` from [[csw.services.location.internal.Networks]]
  *  - `akka.remote.netty.tcp.port` will be a random port or if `isSeed` is true then 3552 (Since cluster seeds will always
  * run on 3552)
  *  - `akka.cluster.seed-nodes` will be self if `isSeed` is true otherwise `clusterSeed` value will be used
  *
  * If none of the Settings are provided then defaults will be picked as follows :
  *  - `akka.remote.netty.tcp.hostname` will be ipV4 address from [[csw.services.location.internal.Networks]]
  *  - `akka.remote.netty.tcp.port` will be a random port
  *  - `akka.cluster.seed-nodes` will be empty
  * and an `ActorSystem` will be created and a cluster will be formed with no Seed Nodes. It will also self join the cluster.
  *
  * `Settings` can be given in three ways :
  *  - by using the api
  *  - by providing system properties
  *  - or by providing environment variables
  *
  * If a `Settings` value e.g. isSeed is provided by more than one ways, then the precedence of consumption will be first from
  *  - System Properties
  *  - then from Environment variable
  *  - and then from `Settings` api
  *
  * @note Although `Settings` can be added through multiple ways, it is recommended that
  *       - `clusterSeed` is provided via environment variable,
  *       - `isSeed` is provided via system properties,
  *       - `interfaceName` is provide via environment variable and
  *       - the `Settings` api of providing values should be used for testing purpose only.
  *
  */
case class Settings(values: Map[String, Any] = Map.empty) {

  /**
    * Name of the `ActorSystem` to join the cluster.
    */
  def name: String = Constants.ClusterName

  val InterfaceNameKey = "interfaceName"
  val ClusterSeedKey = "clusterSeed"
  val IsSeedKey = "isSeed"

  def withEntry(key: String, value: Any): Settings = copy(values = values + (key → value))

  def withInterface(name: String): Settings = withEntry(InterfaceNameKey, name)

  /**
    * Joins the cluster with seed running on localhost
    */
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
