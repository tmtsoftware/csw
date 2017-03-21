package csw.services.location.internal

import java.net.{Inet4Address, InetAddress, NetworkInterface}

import csw.services.location.internal.Networks.NetworkInterfaceNotFound
import org.scalatest.{FunSuite, Matchers}

class NetworksTest extends FunSuite with Matchers{

  test("testGetIpv4Address when interface name is not provided") {
    Networks.getIpv4Address() shouldBe InetAddress.getLocalHost
  }

  test("testGetIpv4Address when interface name is provided") {
    val osName = sys.props.get("os.name")
    osName match {
      case Some(x) if (x.toLowerCase.contains("mac")) => Networks.getIpv4Address("en0") shouldBe a[Inet4Address]
      case Some(x) => Networks.getIpv4Address("eth0") shouldBe a[Inet4Address]
    }
  }

  test("testGetIpv4Address throws NetworkInterfaceNotFound when provided interface name is not present") {
    intercept[NetworkInterfaceNotFound]{
      Networks.getIpv4Address("test")
    }
  }
}
