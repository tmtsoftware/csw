package csw.services.location.internal

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import scala.collection.JavaConverters._

/**
  * An `Exception` that handles NetworkInterface not being present
  *
  * @param message `Message` for exception
  */
case class NetworkInterfaceNotFound(message: String) extends Exception(message)

/**
  * Picks an appropriate ipV4 address to register
  *
  * @param interfaceName   The name of network interface where akka cluster will be formed and an ipV4 address from that
  *                        network interface will be picked.
  * @param networkProvider A custom `NetworkInterfaceProvider` which manages network interfaces and their ip addresses
  */
class Networks(interfaceName: String, networkProvider: NetworkInterfaceProvider) {

  /**
    * Picks an appropriate ipV4 address to register. Default `NetworkInterfaceProvider` will be created to manage network
    * interfaces and their ip addresses
    *
    * @param interfaceName The name of network interface where akka cluster will be formed and an ip address from that network
    *                      interface will be picked
    */
  def this(interfaceName: String) = this(interfaceName, new NetworkInterfaceProvider())

  /**
    * Picks an appropriate ipV4 address to register. Default `NetworkInterfaceProvider` will be created to manage network
    * interfaces and their ip addresses. Since no `interfaceName` is given all network interfaces will be picked to find
    * appropriate ipV4 address
    */
  def this() = this("")

  /**
    * Returns ipv4 host address
    */
  def hostname(): String = getIpv4Address().getHostAddress

  def getIpv4Address(): InetAddress = all()
    .sortBy(_._1)
    .find(pair => isIpv4(pair._2))
    .getOrElse((0, InetAddress.getLocalHost))
    ._2

  private def isIpv4(addr: InetAddress): Boolean = {
    // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
    !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]
  }

  private def all(): Seq[(Int, InetAddress)] = {
    for {
      tuple <- getNetworkInterfaceList()
      a <- tuple._2
    } yield (tuple._1, a)
  }

  private def getNetworkInterfaceList(): Seq[(Int, List[InetAddress])] = {
    if (interfaceName.isEmpty)
      networkProvider.getAllInterfaces()
    else
      networkProvider.getInterface(interfaceName)
  }

}

class NetworkInterfaceProvider() {
  def getAllInterfaces(): Seq[(Int, List[InetAddress])] = {
    NetworkInterface.getNetworkInterfaces.asScala.toList.map(iface => (iface.getIndex, iface.getInetAddresses().asScala.toList))
  }

  def getInterface(interfaceName: String): Seq[(Int, List[InetAddress])] = {
    Option(NetworkInterface.getByName(interfaceName)) match {
      case None                          => throw new NetworkInterfaceNotFound(s"Network interface=${interfaceName} not found.")
      case s@Some(nic: NetworkInterface) => s.map(iface => (iface.getIndex, iface.getInetAddresses().asScala.toList)).get :: Nil
    }
  }
}
