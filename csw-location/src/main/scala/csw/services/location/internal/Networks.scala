package csw.services.location.internal

import java.net.{Inet6Address, InetAddress, NetworkInterface}
import scala.collection.JavaConverters._

import scala.collection.JavaConverters._

object Networks {

  def getIpv4Address(interfaceName: String = ""): InetAddress = Pair.all(interfaceName)
    .sortBy(_.index)
    .find(_.isIpv4)
    .getOrElse(Pair.default)
    .addr

  private case class Pair(index: Int, addr: InetAddress) {
    def isIpv4: Boolean = {
      // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
      !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]
    }
  }

  private object Pair {
    def all(interfaceName: String): List[Pair] = for {
      iface <- getNetworkInterfacesList(interfaceName)
      if iface.isUp && iface.supportsMulticast
      a <- iface.getInetAddresses.asScala
    } yield Pair(iface.getIndex, a)

    def default: Pair = Pair(0, InetAddress.getLocalHost)
  }

  def getNetworkInterfacesList(interfaceName: String): List[NetworkInterface] ={
    if (interfaceName.isEmpty) {
      NetworkInterface.getNetworkInterfaces.asScala.toList
    }
    else {
        Option(NetworkInterface.getByName(interfaceName)) match {
          case None => throw new NetworkInterfaceNotFound(s"Network interface=${interfaceName} not found.")
          case Some(nic : NetworkInterface) => nic::Nil
        }
      }
    }
  case class NetworkInterfaceNotFound(message: String) extends Exception(message)
}
