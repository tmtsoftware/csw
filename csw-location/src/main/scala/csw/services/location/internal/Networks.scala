package csw.services.location.internal

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import scala.collection.JavaConverters._

case class NetworkInterfaceNotFound(message: String) extends Exception(message)

class Networks(interfaceName: String, networkProvider: NetworkInterfaceProvider) {

  def this(interfaceName: String) = this(interfaceName, new NetworkInterfaceProvider())
  def this() = this("")

  def hostname(): String = getIpv4Address().getHostAddress

  def getIpv4Address(): InetAddress = all()
    .sortBy(_._1)
    .find(pair => isIpv4(pair._2))
    .getOrElse((0, InetAddress.getLocalHost))
    ._2

  def isIpv4(addr: InetAddress): Boolean = {
    // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
    !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]
  }

  def all(): Seq[(Int, InetAddress)] = {
    for {
      tuple <- getNetworkInterfaceList()
      a <- tuple._2
    } yield (tuple._1, a)
  }

  def getNetworkInterfaceList(): Seq[(Int, List[InetAddress])] = {
    if (interfaceName.isEmpty)
      networkProvider.getAllInterfaces()
    else
      networkProvider.getInterface(interfaceName)
  }

}

case class NetworkInterfaceDecorator(networkInterface: NetworkInterface) {

}

class NetworkInterfaceProvider() {
  def getAllInterfaces(): Seq[(Int, List[InetAddress])] = {
    NetworkInterface.getNetworkInterfaces.asScala.toList.map(iface=>(iface.getIndex, iface.getInetAddresses().asScala.toList))
  }

  def getInterface(interfaceName: String): Seq[(Int, List[InetAddress])] = {
    Option(NetworkInterface.getByName(interfaceName)) match {
      case None => throw new NetworkInterfaceNotFound(s"Network interface=${interfaceName} not found.")
      case s@Some(nic:NetworkInterface) => s.map(iface=>(iface.getIndex, iface.getInetAddresses().asScala.toList)).get :: Nil
    }
  }
}
