package csw.services.location.internal

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.location.Connection.TcpConnection
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.models.TcpRegistration
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.FunSuite

class LocationServiceClientTest extends FunSuite {
  implicit val actorSystem: ActorSystem        = ActorSystemFactory.remote("demo")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val locationServiceClient            = new LocationServiceClient()

  private val registration = TcpRegistration(
    TcpConnection(ComponentId("aws", ComponentType.Service)),
    3456,
    LogAdminActorFactory.make(actorSystem)
  )

  test("demo") {
    println(locationServiceClient.list.await)
    val registrationResult = locationServiceClient.register(registration).await
    Thread.sleep(2000)
    println(locationServiceClient.list.await)
    registrationResult.unregister().await
    println(locationServiceClient.list.await)
  }
}
