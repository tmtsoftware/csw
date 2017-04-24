package csw.services.config.client.internal

import java.net.URI

import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType, HttpLocation}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ConfigServiceResolverTest extends FunSuite with Matchers with MockitoSugar {
  private val configConnection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
  private val actorRuntime     = new ActorRuntime()
  import actorRuntime._

  test("should throw exception if not able to resolve config service http server") {
    val locationService = LocationServiceFactory.make()
    val configService   = ConfigClientFactory.make(actorSystem, locationService)

    val exception = intercept[RuntimeException] {
      Await.result(configService.list(), 7.seconds)
    }

    exception.getMessage shouldEqual (s"config service connection=${configConnection.name} can not be resolved")
  }

  test("should give URI for resolved config service") {

    val mockedLocationService  = mock[LocationService]
    val uri                    = new URI(s"http://config-host:4000")
    val resolvedConfigLocation = Future(Some(HttpLocation(configConnection, uri)))
    when(mockedLocationService.resolve(configConnection, 5.seconds)).thenReturn(resolvedConfigLocation)

    val configServiceResolver = new ConfigServiceResolver(mockedLocationService, actorRuntime)
    val actualUri             = Await.result(configServiceResolver.uri, 2.seconds)

    actualUri.toString() shouldEqual uri.toString
  }
}
