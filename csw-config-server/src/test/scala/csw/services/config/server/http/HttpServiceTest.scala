package csw.services.config.server.http

import akka.stream.BindFailedException
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.commons.{ConfigServiceConnection, RegistrationFactory}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.location.api.exceptions.OtherLocationIsRegistered
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.util.control.NonFatal

class HttpServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("should start the http server and register with location service") {
    val _servicePort = 4005
    val serverWiring = ServerWiring.make(ClusterAwareSettings, Some(_servicePort))
    import serverWiring._
    val (binding, registrationResult) = httpService.registeredLazyBinding.await
    locationService.find(ConfigServiceConnection.value).await.get.connection shouldBe ConfigServiceConnection.value

    binding.localAddress.getAddress.getHostAddress shouldBe ClusterAwareSettings.hostname
    registrationResult.location.connection shouldBe ConfigServiceConnection.value
    actorRuntime.shutdown(TestFinishedReason).await
  }

  test("should not register with location service if server binding fails") {
    val _servicePort     = 4006
    val locationService1 = LocationServiceFactory.withSettings(ClusterSettings().onPort(_servicePort))
    val _clusterSettings = ClusterSettings().joinLocal(_servicePort)
    val serverWiring     = ServerWiring.make(_clusterSettings, Some(_servicePort))

    import serverWiring._

    intercept[BindFailedException] {
      httpService.registeredLazyBinding.await
    }

    locationService1.find(ConfigServiceConnection.value).await shouldBe None
    locationService1.shutdown(TestFinishedReason)
  }

  test("should not start server if registration with location service fails") {
    val _servicePort = 4007
    val serverWiring = ServerWiring.make(ClusterAwareSettings, Some(_servicePort))
    import serverWiring._
    locationService.register(RegistrationFactory.http(ConfigServiceConnection.value, 21212, "")).await

    locationService.find(ConfigServiceConnection.value).await.get.connection shouldBe ConfigServiceConnection.value

    intercept[OtherLocationIsRegistered] {
      httpService.registeredLazyBinding.await
    }

    //TODO: Find a way to assert server is not bounded
    try {
      actorRuntime.shutdown(TestFinishedReason).await
    } catch {
      case NonFatal(ex) ⇒
    }
  }
}
