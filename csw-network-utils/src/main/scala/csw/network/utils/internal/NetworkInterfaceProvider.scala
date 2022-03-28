/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.network.utils.internal

import java.net.{InetAddress, NetworkInterface}

import csw.network.utils.exceptions.NetworkInterfaceNotFound

import scala.jdk.CollectionConverters._

/**
 *  Provides InetAddresses for network interface
 */
private[network] class NetworkInterfaceProvider {

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
        throw NetworkInterfaceNotFound(s"Network interface=$interfaceName not found")
    }
}
