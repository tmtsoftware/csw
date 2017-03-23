package csw.services.location.internal

import java.net.{Inet4Address, InetAddress, NetworkInterface}

import csw.services.location.internal.Networks.NetworkInterfaceNotFound
import org.scalatest.{FunSuite, Matchers}
import scala.collection.JavaConverters._

class NetworksTest extends FunSuite with Matchers{

  test("testGetIpv4Address when interface name is not provided") {
    Networks.getIpv4Address() shouldBe InetAddress.getLocalHost
  }

  test("testGetIpv4Address when interface name is provided") {
    val allInterfaces = NetworkInterface.getNetworkInterfaces.asScala.toList.toString()
    if (allInterfaces.toLowerCase.contains("en0")) Networks.getIpv4Address("en0") shouldBe a[Inet4Address]
    if (allInterfaces.toLowerCase.contains("ens3")) Networks.getIpv4Address("ens3") shouldBe a[Inet4Address]
    if (allInterfaces.toLowerCase.contains("eth0")) Networks.getIpv4Address("eth0") shouldBe a[Inet4Address]
  }

  test("testGetIpv4Address throws NetworkInterfaceNotFound when provided interface name is not present") {
    intercept[NetworkInterfaceNotFound]{
      Networks.getIpv4Address("test")
    }
  }
}
