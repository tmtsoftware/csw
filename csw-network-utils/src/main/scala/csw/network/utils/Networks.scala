package csw.network.utils

import java.net.{Inet6Address, InetAddress}

import csw.network.utils.internal.NetworkInterfaceProvider

/**
 * Picks an appropriate ipv4 address to register using the NetworkInterfaceProvider
 *
 * @param interfaceName provide the name of network interface where csw cluster is running
 */
case class Networks(interfaceName: String, networkProvider: NetworkInterfaceProvider) {

  /**
   * Gives the ipv4 host address
   */
  def hostname: String = ipv4Address.getHostAddress

  /**
   * Gives the non-loopback, ipv4 address for the given network interface. If no interface name is provided then the address mapped
   * to the first available interface is chosen.
   */
  private[network] def ipv4Address: InetAddress =
    mappings
      .sortBy(_._1)
      .find(pair => isIpv4(pair._2))
      .getOrElse((0, InetAddress.getLocalHost))
      ._2

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

  /**
   * Picks an appropriate ipv4 address from the network interface provided.
   * If no specific network interface is provided, the first available interface will be taken to pick address
   */
  def apply(interfaceName: String): Networks = new Networks(interfaceName, new NetworkInterfaceProvider)

  def apply(): Networks = new Networks((sys.env ++ sys.props).getOrElse("interfaceName", ""), new NetworkInterfaceProvider)
}
