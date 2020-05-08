package csw.location.server.internal

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration, NetworkType}
import csw.location.server.commons.CswCluster
import csw.prefix.models.{Prefix, Subsystem}
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LocationServiceImplTest extends AnyFunSuite with Matchers with MockitoSugar {
  private val mockCswCluster = mock[CswCluster]
  private val httpConnection = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "ConfigServer"), ComponentType.Service))
  private val port           = 5003

  test("should register public hostname when network type is public | CSW-97") {
    val registration = HttpRegistration(connection = httpConnection, port = port, path = "", NetworkType.Public)

    when(mockCswCluster.publicHostname) thenReturn ("some-public-ip")

    val locationService = new LocationServiceImpl(mockCswCluster)
    locationService.getLocation(registration).uri.getHost shouldBe "some-public-ip"
  }

  test("should register private hostname when no network type provided | CSW-97") {
    val registration = HttpRegistration(connection = httpConnection, port = port, path = "")

    when(mockCswCluster.hostname) thenReturn ("some-private-ip")

    val locationService = new LocationServiceImpl(mockCswCluster)
    locationService.getLocation(registration).uri.getHost shouldBe "some-private-ip"
  }
}
