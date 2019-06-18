package csw.network.utils

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import com.typesafe.config.ConfigFactory
import csw.logging.api.scaladsl.Logger
import csw.network.utils.commons.NetworksLogger
import csw.network.utils.exceptions.NetworkInterfaceNotProvided
import csw.network.utils.internal.NetworkInterfaceProvider

/**
 * Picks an appropriate ipv4 address to register using the NetworkInterfaceProvider
 *
 * @param interfaceName provide the name of network interface where csw cluster is running
 */
private[csw] case class Networks(private val interfaceName: String, networkProvider: NetworkInterfaceProvider) {

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

  //Get a flattened seq of Index -> InetAddresses pairs
  private def mappings: Seq[(Int, InetAddress)] =
    for {
      (index, inetAddresses) ← interfaces
      inetAddress            ← inetAddresses
    } yield (index, inetAddress)

  private def interfaces: Seq[(Int, List[InetAddress])] =
    if (interfaceName.isEmpty) networkProvider.allInterfaces
    else networkProvider.getInterface(interfaceName)

}

object Networks {

  private val log: Logger = NetworksLogger.getLogger

  /* ======== Testing API ========
   * config property `csw-networks.hostname.automatic` is enabled only in test scope to automatically detect appropriate hostname
   * so that we do not need to set INTERFACE_NAME env variable in every test or globally on machine before running tests.
   */
  private def fallbackInterfaceName =
    if (automatic) ""
    else {
      val networkInterfaceNotProvided = NetworkInterfaceNotProvided("INTERFACE_NAME env variable is not set.")
      log.error(networkInterfaceNotProvided.message, ex = networkInterfaceNotProvided)
      throw networkInterfaceNotProvided
    }

  /**
   * Creates instance of `Networks` by reading `INTERFACE_NAME` env variable
   */
  def apply(): Networks = apply(None)

  /**
   * Picks an appropriate ipv4 address from the network interface provided.
   * If no specific network interface is provided, the first available interface will be taken to pick address
   */
  def apply(interfaceName: Option[String]): Networks = {
    val ifaceName = interfaceName match {
      case Some(interface) ⇒ interface
      case None            ⇒ (sys.env ++ sys.props).getOrElse("INTERFACE_NAME", fallbackInterfaceName)
    }
    new Networks(ifaceName, new NetworkInterfaceProvider)
  }

  private def automatic: Boolean = ConfigFactory.load().getBoolean("csw-networks.hostname.automatic")

}
