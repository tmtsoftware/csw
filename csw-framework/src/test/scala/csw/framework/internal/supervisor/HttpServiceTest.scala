package csw.framework.internal.supervisor

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{HttpRegistration, NetworkType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.network.utils.Networks
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class HttpServiceTest extends AnyWordSpecLike with MockitoSugar with ScalaFutures with Matchers {

  private val locationService                                          = mock[LocationService]
  private val route                                                    = mock[Route]
  private val logger: Logger                                           = mock[Logger]
  private val httpConnection                                           = mock[HttpConnection]
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  implicit override val patienceConfig: PatienceConfig                 = PatienceConfig(10.seconds, 100.millis)
  private val hostname                                                 = Networks().hostname

  "Embedded Http Server" must {
    "Bind and register to Private Network Type | CSW-96" in {
      val httpService                        = new HttpService(locationService, route, logger, httpConnection)
      val result: Future[RegistrationResult] = Future.successful(mock[RegistrationResult])
      when(locationService.register(any[HttpRegistration])).thenReturn(result)

      val (binding, _) = httpService.bindAndRegister().futureValue

      val captor = ArgCaptor[HttpRegistration]

      binding.localAddress.getAddress.getHostAddress shouldBe hostname
      verify(locationService).register(captor)
      captor.value.networkType shouldBe NetworkType.Private
    }
  }
}
