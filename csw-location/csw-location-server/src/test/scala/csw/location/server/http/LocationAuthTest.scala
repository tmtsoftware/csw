package csw.location.server.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.core.commons.AASConnection
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.HttpRegistration
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.TestFutureExtension._
import csw.network.utils.SocketUtils
import msocket.api.models.ServiceError
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class LocationAuthTest
    extends HTTPLocationServiceOnPorts(SocketUtils.getFreePort, SocketUtils.getFreePort, auth = true)
    with AnyFunSuiteLike
    with Matchers
    with LocationServiceCodecs {

  private implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = locationWiring.get.actorSystem

  test("register (protected route) should return AASResolutionFailed when keycloak is not yet registered") {
    val locationNoAuthClient = HttpLocationServiceFactory.make("localhost", httpPort)
    val aasPort              = 5675
    val serviceError =
      intercept[ServiceError](locationNoAuthClient.register(HttpRegistration(AASConnection.value, aasPort, "")).await)
    serviceError.generic_error.errorName shouldBe "AASResolutionFailed"
  }
}
