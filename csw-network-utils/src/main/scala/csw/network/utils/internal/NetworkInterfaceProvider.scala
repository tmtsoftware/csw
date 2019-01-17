package csw.network.utils.internal

import java.net.{InetAddress, NetworkInterface}

import csw.logging.api.scaladsl.Logger
import csw.network.utils.commons.NetworksLogger
import csw.network.utils.exceptions.NetworkInterfaceNotFound

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

/**
 *  Provides InetAddresses for network interface
 */
private[network] class NetworkInterfaceProvider {

  private val log: Logger = NetworksLogger.getLogger

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
