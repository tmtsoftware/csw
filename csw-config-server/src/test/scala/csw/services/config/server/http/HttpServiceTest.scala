package csw.services.config.server.http

import akka.stream.BindFailedException
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.ConfigServiceConnection
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.ClusterSettings
import csw.services.location.exceptions.OtherLocationIsRegistered
import csw.services.location.internal.Networks
import csw.services.location.models.HttpRegistration
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.util.control.NonFatal

class HttpServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("should start the http server and register with location service") {
    val serverWiring = new ServerWiring
    import serverWiring._
    val (binding, registrationResult) = httpService.registeredLazyBinding.await
    locationService.find(ConfigServiceConnection).await.get.connection shouldBe ConfigServiceConnection

    binding.localAddress.getAddress.getHostAddress shouldBe new Networks().hostname()
    registrationResult.location.connection shouldBe ConfigServiceConnection
    actorRuntime.shutdown().await
  }

  test("should not register with location service if server binding fails") {
    val _servicePort     = 4001
    val locationService1 = LocationServiceFactory.withSettings(ClusterSettings().onPort(_servicePort))
    val _clusterSettings = ClusterSettings().joinLocal(_servicePort)
    val serverWiring     = ServerWiring.make(_clusterSettings, Some(_servicePort))

    import serverWiring._

    intercept[BindFailedException] {
      httpService.registeredLazyBinding.await
    }

    locationService1.find(ConfigServiceConnection).await shouldBe None
    locationService1.shutdown()
    try {
      actorRuntime.shutdown().await
    } catch {
      case NonFatal(ex) ⇒ println(ex.getMessage)
    }
  }

  test("should not start server if registration with location service fails") {
    val serverWiring = new ServerWiring
    import serverWiring._
    locationService.register(HttpRegistration(ConfigServiceConnection, 21212, "")).await

    locationService.find(ConfigServiceConnection).await.get.connection shouldBe ConfigServiceConnection

    intercept[OtherLocationIsRegistered] {
      httpService.registeredLazyBinding.await
    }

    //TODO: Find a way to assert server is not bounded
    try {
      actorRuntime.shutdown().await
    } catch {
      case NonFatal(ex) ⇒ println(ex.getMessage)
    }
  }
}
