package csw.services.location.internal

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import scala.collection.JavaConverters._

case class NetworkInterfaceNotFound(message: String) extends Exception(message)

/**
  * Picks an appropriate ipv4 address to register using the NetworkInterfaceProvider
  *
  * @param interfaceName Provide the name of network interface where csw cluster is running
  */
class Networks(interfaceName: String, networkProvider: NetworkInterfaceProvider) {

  /**
    * Picks an appropriate ipv4 address from the network interface provided
    */
  def this(interfaceName: String) = this(interfaceName, new NetworkInterfaceProvider())

  /**
    * Picks an appropriate ipv4 address. Since no specific network interface is provided, the first available interface will be
    * taken to pick address
    */
  def this() = this("")

  /**
    * Gives the ipv4 host address
     */
  def hostname(): String = getIpv4Address().getHostAddress

  /**
    * Gives the non-loopback, ipv4 address for the given network interface. If no interface name is provided then the address mapped
    * to the first available interface is chosen.
    */
  def getIpv4Address(): InetAddress = all()
    .sortBy(_._1)
    .find(pair => isIpv4(pair._2))
    .getOrElse((0, InetAddress.getLocalHost))
    ._2

  // Check if the given InetAddress is not a loopback address and is a ipv4 address
  private def isIpv4(addr: InetAddress): Boolean = {
    // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
    !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]
  }

  // Prepare a tuple of interface index to InetAddress
  private def all(): Seq[(Int, InetAddress)] = {
    for {
      tuple <- getNetworkInterfaceList()
      a <- tuple._2
    } yield (tuple._1, a)
  }

  // If the network interface is defined then get all InetAddresses mapped for that else get it for all interfaces
  private def getNetworkInterfaceList(): Seq[(Int, List[InetAddress])] = {
    if (interfaceName.isEmpty)
      networkProvider.getAllInterfaces()
    else
      networkProvider.getInterface(interfaceName)
  }

}

/**
  *  Provides InetAddresses for network interface
   */
class NetworkInterfaceProvider() {

  /**
    * Prepare a Sequence of tuple where each tuple represents a network interface and the list of all InetAddresses mapped for that
    * interface
    */
  def getAllInterfaces(): Seq[(Int, List[InetAddress])] = {
    NetworkInterface.getNetworkInterfaces.asScala.toList.map(iface => (iface.getIndex, iface.getInetAddresses().asScala.toList))
  }

  /**
    * Prepare a tuple representing the given interface and the list of all InetAddresses mapped for that interface. If the given
    * interface is not found then an exception is thrown
    */
  def getInterface(interfaceName: String): Seq[(Int, List[InetAddress])] = {
    Option(NetworkInterface.getByName(interfaceName)) match {
      case None                          => throw new NetworkInterfaceNotFound(s"Network interface=${interfaceName} not found.")
      case s@Some(nic: NetworkInterface) => s.map(iface => (iface.getIndex, iface.getInetAddresses().asScala.toList)).get :: Nil
    }
  }
}
