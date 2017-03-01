package csw.services.location.common

import scala.collection.JavaConverters._
import java.net.{Inet6Address, InetAddress, NetworkInterface}

object Networks {

  def getPrimaryIpv4Address: InetAddress = Pair.all
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
    def all: List[Pair] = for {
      iface <- NetworkInterface.getNetworkInterfaces.asScala.toList
      if iface.isUp && iface.supportsMulticast
      a <- iface.getInetAddresses.asScala
    } yield Pair(iface.getIndex, a)

    def default: Pair = Pair(0, InetAddress.getLocalHost)
  }
}
