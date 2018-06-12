package csw.services.location.internal

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.messages.location.Connection.TcpConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.LocationService
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.FunSuite

class LocationServiceClientTest extends FunSuite with LocationJsonSupport {
  implicit val actorSystem: ActorSystem        = ActorSystemFactory.remote("demo")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val locationService: LocationService = new LocationServiceClient()

  private val tcpConnection = TcpConnection(ComponentId("aws", ComponentType.Service))
  private val registration = TcpRegistration(
    tcpConnection,
    3456,
    LogAdminActorFactory.make(actorSystem)
  )

  test("demo1") {
    println(locationService.list.await)
    val registrationResult = locationService.register(registration).await
    println(locationService.list.await)
//    registrationResult.unregister().await
    locationService.unregisterAll().await
    println(locationService.list.await)
  }

  test("demo2") {
    val registrationResult = locationService.register(registration).await
    println(locationService.list.await)
    println(locationService.find(tcpConnection).await)
    registrationResult.unregister().await
    println(locationService.list.await)
  }
}
