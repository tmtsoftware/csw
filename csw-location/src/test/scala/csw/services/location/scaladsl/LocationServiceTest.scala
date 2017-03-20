package csw.services.location.scaladsl

import javax.jmdns.JmDNS

import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.internal.{LocationEventStream, LocationServiceImpl}
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpRegistration}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Future
import scala.util.Failure
import org.mockito.Mockito._

class LocationServiceTest
  extends FunSuite
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll {

  val actorRuntime = new ActorRuntime("test")

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }
  
  test("future should contain a exception in case jmDNS throws an error") {
    val registration = TcpRegistration(TcpConnection(ComponentId("", ComponentType.HCD)), 100)

    val jmDNS = mock[JmDNS]
    val locationEventStream = mock[LocationEventStream]

    when(jmDNS.registerService(registration.serviceInfo)).thenThrow(CustomExp)
    when(locationEventStream.list).thenReturn(Future.successful(List.empty))

    val locationService: LocationService = new LocationServiceImpl(jmDNS, actorRuntime, locationEventStream)

    locationService.register(registration).done.value.get shouldBe Failure(CustomExp)
  }

  case object CustomExp extends RuntimeException

}
