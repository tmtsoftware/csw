package csw.services.location.internal

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import csw.services.location.commons.LocationServiceLogger

import scala.collection.JavaConverters._

case class NetworkInterfaceNotFound(message: String) extends Exception(message)

/**
 * Picks an appropriate ipv4 address to register using the NetworkInterfaceProvider
 *
 * @param interfaceName Provide the name of network interface where csw cluster is running
 */
class Networks(interfaceName: String, networkProvider: NetworkInterfaceProvider) extends LocationServiceLogger.Simple {

  /**
   * Picks an appropriate ipv4 address from the network interface provided
   */
  def this(interfaceName: String) = this(interfaceName, new NetworkInterfaceProvider)

  /**
   * Picks an appropriate ipv4 address. Since no specific network interface is provided, the first available interface will be
   * taken to pick address
   */
  def this() = this("")

  /**
   * Gives the ipv4 host address
   */
  def hostname(): String = {
    log.info(s"Fetching hostname for interface $interfaceName")
    ipv4Address.getHostAddress
  }

  /**
   * Gives the non-loopback, ipv4 address for the given network interface. If no interface name is provided then the address mapped
   * to the first available interface is chosen.
   */
  def ipv4Address: InetAddress =
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
      (index, inetAddresses) <- interfaces
      inetAddress            <- inetAddresses
    } yield (index, inetAddress)

  private def interfaces: Seq[(Int, List[InetAddress])] =
    if (interfaceName.isEmpty)
      networkProvider.allInterfaces
    else
      networkProvider.getInterface(interfaceName)

}

/**
 *  Provides InetAddresses for network interface
 */
class NetworkInterfaceProvider extends LocationServiceLogger.Simple {

  /**
   * Get Seq of (Index -> List of InetAddress) mapping for each interface
   */
  def allInterfaces: Seq[(Int, List[InetAddress])] =
    NetworkInterface.getNetworkInterfaces.asScala.toList
      .map(iface => (iface.getIndex, iface.getInetAddresses.asScala.toList))

  /**
   * Get Seq of (Index -> List of InetAddress) mapping for a given interface
   */
  def getInterface(interfaceName: String): Seq[(Int, List[InetAddress])] =
    Option(NetworkInterface.getByName(interfaceName)) match {
      case Some(nic) => List((nic.getIndex, nic.getInetAddresses.asScala.toList))
      case None =>
        val networkInterfaceNotFound = NetworkInterfaceNotFound(s"Network interface=$interfaceName not found")
        log.error(networkInterfaceNotFound.getMessage, ex = networkInterfaceNotFound)
        throw networkInterfaceNotFound
    }
}
