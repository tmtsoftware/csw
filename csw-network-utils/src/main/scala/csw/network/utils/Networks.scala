package csw.network.utils

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import com.typesafe.config.ConfigFactory
import csw.network.utils.exceptions.NetworkInterfaceNotProvided
import csw.network.utils.internal.NetworkInterfaceProvider

/**
 * Picks an appropriate ipv4 address to register using the NetworkInterfaceProvider
 *
 * @param interfaceName provide the name of network interface where csw cluster is running
 */
case class Networks(private val interfaceName: String, networkProvider: NetworkInterfaceProvider) {

  private val (_, hostName) = ipv4AddressWithInterfaceName

  /**
   * Gives the ipv4 host address
   */
  def hostname: String = hostName.getHostAddress

  /**
   * Gives the non-loopback, ipv4 address for the given network interface. If no interface name is provided then the address mapped
   * to the first available interface is chosen.
   */
  private[network] def ipv4AddressWithInterfaceName: (String, InetAddress) =
    mappings
      .sortBy(_._1)
      .find(pair => isIpv4(pair._2))
      .map { case (index, ip) => (NetworkInterface.getByIndex(index).getName, ip) }
      .getOrElse(("LocalHost", InetAddress.getLocalHost))

  // Check if the given InetAddress is not a loopback address and is a ipv4 address
  private def isIpv4(addr: InetAddress): Boolean =
    // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
    !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]

  // Get a flattened seq of Index -> InetAddresses pairs
  private def mappings: Seq[(Int, InetAddress)] =
    for {
      (index, inetAddresses) <- interfaces
      inetAddress            <- inetAddresses
    } yield (index, inetAddress)

  private def interfaces: Seq[(Int, List[InetAddress])] =
    if (interfaceName.isEmpty) networkProvider.allInterfaces
    else networkProvider.getInterface(interfaceName)

}

object Networks {

  /**
   * Creates instance of `Networks` by reading given env variable, if not given default is INTERFACE_NAME
   */
  def apply(interfaceNameEnvKey: String = "INTERFACE_NAME"): Networks = {
    createNetwork(readInterfaceNameFromEnv(interfaceNameEnvKey))
  }

  /**
   * Picks an appropriate ipv4 address from the network interface provided.
   * If None interfaceName provided, it will try to read from env variable INTERFACE_NAME
   * In tests(csw-networks.hostname.automatic=on), If no specific network interface is provided, the first available
   * interface will be taken to pick address
   */
  def interface(interfaceName: Option[String]): Networks =
    getNetwork(interfaceName, fallbackEnvKey = "INTERFACE_NAME")

  /**
   * Picks an appropriate ipv4 address from the network interface provided.
   * If None interfaceName provided, it will try to read from env variable AAS_INTERFACE_NAME
   * In tests(csw-networks.hostname.automatic=on), If no specific network interface is provided, the first available
   * interface will be taken to pick address
   */
  def publicInterface(interfaceName: Option[String]): Networks =
    getNetwork(interfaceName, fallbackEnvKey = "AAS_INTERFACE_NAME")

  private def getNetwork(interfaceName: Option[String], fallbackEnvKey: String): Networks =
    interfaceName match {
      case Some(interfaceName) => createNetwork(interfaceName)
      case None                => createNetwork(readInterfaceNameFromEnv(fallbackEnvKey))
    }

  private def readInterfaceNameFromEnv(envKey: String): String = {
    (sys.env ++ sys.props).getOrElse(envKey, fallbackInterfaceName(envKey))
  }

  private def createNetwork(interfaceName: String): Networks = {
    new Networks(interfaceName, new NetworkInterfaceProvider)
  }

  private def automatic: Boolean = ConfigFactory.load().getBoolean("csw-networks.hostname.automatic")

  /* ======== Testing API ========
   * config property `csw-networks.hostname.automatic` is enabled only in test scope to automatically detect appropriate hostname
   * so that we do not need to set INTERFACE_NAME env variable in every test or globally on machine before running tests.
   */
  private def fallbackInterfaceName(interfaceName: String): String =
    if (automatic) ""
    else throw NetworkInterfaceNotProvided(s"$interfaceName env variable is not set.")
}
