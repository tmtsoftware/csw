package csw.network.utils

import java.net.{InetAddress, NetworkInterface}
import csw.network.utils.exceptions.{NetworkInterfaceNotFound, NetworkInterfaceNotProvided}
import csw.network.utils.internal.NetworkInterfaceProvider
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar

import scala.jdk.CollectionConverters.*

class NetworksTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar {

  // DEOPSCSW-673: Make interfaceName environment variable mandatory
  test(
    "Networks() should throw NetworkInterfaceNotProvided when INTERFACE_NAME env variable is not set" +
      " | DEOPSCSW-673 , DEOPSCSW-97"
  ) {
    val networkInterfaceNotProvided = intercept[NetworkInterfaceNotProvided] {
      Networks()
    }
    networkInterfaceNotProvided.getMessage shouldBe "INTERFACE_NAME env variable is not set."
  }

  test(
    "Networks(some-interface-name) should throw NetworkInterfaceNotProvided when provided interface name env variable is not set " +
      "| CSW-97"
  ) {
    val networkInterfaceNotProvided = intercept[NetworkInterfaceNotProvided] {
      Networks("some-interface-name")
    }
    networkInterfaceNotProvided.getMessage shouldBe "some-interface-name env variable is not set."
  }

  test("Networks.interface() throws NetworkInterfaceNotFound when provided interface name is not present/valid | CSW-97") {
    val networkInterfaceNotFound = intercept[NetworkInterfaceNotFound] {
      Networks.interface(Some("test"))
    }
    networkInterfaceNotFound.getMessage shouldBe "Network interface=test not found"
  }

  test(
    "Networks.publicInterface() throws NetworkInterfaceNotFound when provided public interface name is not present/valid | CSW-97"
  ) {
    val networkInterfaceNotFound = intercept[NetworkInterfaceNotFound] {
      Networks.publicInterface(Some("test"))
    }
    networkInterfaceNotFound.getMessage shouldBe "Network interface=test not found"
  }

  test("Networks.interface() throws NetworkInterfaceNotProvided when interface name is not given | CSW-97") {
    val networkInterfaceNotProvided = intercept[NetworkInterfaceNotProvided] {
      Networks.interface(None)
    }
    networkInterfaceNotProvided.getMessage shouldBe "INTERFACE_NAME env variable is not set."
  }

  test(
    "Networks.publicInterface() throws NetworkInterfaceNotProvided when public interface name is not given | CSW-97"
  ) {
    val networkInterfaceNotProvided = intercept[NetworkInterfaceNotProvided] {
      Networks.publicInterface(None)
    }
    networkInterfaceNotProvided.getMessage shouldBe "AAS_INTERFACE_NAME env variable is not set."
  }

  test("Should filter ipv6 addresses | ") {
    val mockedNetworkProvider = mock[NetworkInterfaceProvider]
    val inet4Address =
      InetAddress.getByAddress(Array[Byte](192.toByte, 168.toByte, 1, 2))
    val inet6Address = InetAddress.getByAddress(
      Array[Byte](
        192.toByte,
        168.toByte,
        1,
        2,
        192.toByte,
        168.toByte,
        1,
        2,
        192.toByte,
        168.toByte,
        1,
        2,
        192.toByte,
        168.toByte,
        1,
        2
      )
    )
    when(mockedNetworkProvider.getInterface("eth0"))
      .thenReturn(Seq((1, List(inet6Address, inet4Address))))
    val ipv4Address: InetAddress =
      new Networks("eth0", mockedNetworkProvider).ipv4AddressWithInterfaceName._2
    ipv4Address shouldEqual inet4Address

  }

  // this is testing internal Networks API bypassing INTERFACE_NAME env variable
  test(
    "Should get ip4 address of interface with lowest index when INTERFACE_NAME env variable is not set (simulates automatic interface detection)"
  ) {
    val inet4Address1 =
      InetAddress.getByAddress(Array[Byte](192.toByte, 168.toByte, 1, 2))
    val inet4Address2 =
      InetAddress.getByAddress(Array[Byte](172.toByte, 17.toByte, 1, 2))
    val inet4Address3 =
      InetAddress.getByAddress(Array[Byte](10.toByte, 12.toByte, 2, 1))
    val mockedNetworkProvider = mock[NetworkInterfaceProvider]
    when(mockedNetworkProvider.allInterfaces)
      .thenReturn(Seq((1, List(inet4Address1)), (2, List(inet4Address2)), (3, List(inet4Address3))))

    Networks("", mockedNetworkProvider).ipv4AddressWithInterfaceName._2 shouldBe inet4Address1
  }

  test("test" + "GetIpv4Address returns inet address when provided a valid interface name | ") {
    val inetAddresses: List[(String, InetAddress)] =
      NetworkInterface.getNetworkInterfaces.asScala.toList.map { iface =>
        Networks.interface(Some(iface.getName)).ipv4AddressWithInterfaceName
      }

    inetAddresses.contains(("LocalHost", InetAddress.getLocalHost)) shouldEqual true
  }
}
