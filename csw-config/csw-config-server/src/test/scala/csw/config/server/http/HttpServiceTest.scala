/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.core.commons.AASConnection
import csw.config.server.ServerWiring
import csw.config.server.commons.ConfigServiceConnection
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models
import csw.location.api.models.NetworkType
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import csw.network.utils.{Networks, SocketUtils}

import scala.concurrent.duration.DurationDouble
import scala.util.control.NonFatal

class HttpServiceTest extends HTTPLocationService {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  private val testLocationService: LocationService        = HttpLocationServiceFactory.makeLocalClient
  implicit override val patienceConfig: PatienceConfig    = PatienceConfig(10.seconds, 100.millis)

  // register AAS with location service
  private val AASPort = 8080

  override def beforeAll(): Unit = {
    super.beforeAll()
    testLocationService.register(models.HttpRegistration(AASConnection.value, AASPort, "auth")).futureValue
  }

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
    super.afterAll()
  }

  // CSW-97
  /*
   * Tests can be run of different OS and each OS follow its own naming convention for interface names.
   * e.g. for Mac OS  interface names are like en0, en1 ,etc. For Linux they are eth0, eth1, etc. and so on.
   * Hence, it is not feasible to set and use env variable - NetworkType.Inside.envKey and NetworkType.Outside.envKey
   * in tests, as they are machine dependent.
   * Instead, a config property `csw-networks.hostname.automatic` is enabled in test scope to automatically detect
   * appropriate interface and hostname, which means Networks().hostname and Networks(NetworkType.Public.envKey)
   * .hostname will be same in tests.
   */
  private val hostname: String = Networks(NetworkType.Outside.envKey).hostname

  test("should bind the http server and register it with location service | CSW-97") {
    val _servicePort = 4005
    val serverWiring = ServerWiring.make(Some(_servicePort))
    import serverWiring._

    SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false

    val (_, registrationResult) = httpService.registeredLazyBinding.futureValue

    locationService.find(ConfigServiceConnection.value).futureValue.get.connection shouldBe ConfigServiceConnection.value
    val location = registrationResult.location
    location.uri.getHost shouldBe hostname
    location.uri.getPort shouldBe _servicePort
    location.connection shouldBe ConfigServiceConnection.value
    // should not bind to all but specific hostname IP
    SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe true
    SocketUtils.isAddressInUse("localhost", _servicePort) shouldBe false
    actorRuntime.shutdown().futureValue
  }

  test("should not register with location service if server binding fails | CSW-97") {
    val _servicePort    = 3553 // Location Service runs on this port
    val serverWiring    = ServerWiring.make(Some(_servicePort))
    val address         = s"[/${hostname}:${_servicePort}]"
    val expectedMessage = s"Bind failed because of java.net.BindException: $address Address already in use"
    import serverWiring._

    val bindException = intercept[Exception] { httpService.registeredLazyBinding.futureValue }

    bindException.getCause.getMessage shouldBe expectedMessage
    testLocationService.find(ConfigServiceConnection.value).futureValue shouldBe None
  }

  test("should not start server if registration with location service fails") {
    val _existingServicePort = 21212
    val _servicePort         = 4007
    val serverWiring         = ServerWiring.make(Some(_servicePort))
    import serverWiring._
    locationService
      .register(models.HttpRegistration(ConfigServiceConnection.value, _existingServicePort, "", NetworkType.Outside))
      .futureValue
    locationService.find(ConfigServiceConnection.value).futureValue.get.connection shouldBe ConfigServiceConnection.value

    SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false

    val otherLocationIsRegistered = intercept[Exception] { httpService.registeredLazyBinding.futureValue }

    otherLocationIsRegistered.getCause shouldBe a[OtherLocationIsRegistered]
    SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false
    try actorRuntime.shutdown().futureValue
    catch {
      case NonFatal(_) =>
    }
  }
}
