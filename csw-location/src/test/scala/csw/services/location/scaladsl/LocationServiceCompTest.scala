package csw.services.location.scaladsl

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.common.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalatest._

import scala.concurrent.Future

class LocationServiceCompTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val actorRuntimePort = 2554

  private val actorRuntime = new ActorRuntime("test", Map("akka.remote.netty.tcp.port" -> actorRuntimePort))
  private val locationService = LocationServiceFactory.make(actorRuntime)

  override protected def afterEach(): Unit = {
    locationService.unregisterAll()
  }

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }

  test("tcp location") {

    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    registrationResult.componentId shouldBe componentId

    val uri = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:$Port")

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, uri)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }
  // #hello_example
  test("http location") {
    val Port = 1234
    val componentId = ComponentId("configService", ComponentType.Service)
    val connection = HttpConnection(componentId)
    val Path = "path123"

    val registrationResult = locationService.register(HttpRegistration(connection, Port, Path)).await

    registrationResult.componentId shouldBe componentId

    val uri = new URI(s"http://${Networks.getPrimaryIpv4Address.getHostAddress}:$Port/$Path")

    locationService.list.await shouldBe List(
      ResolvedHttpLocation(connection, uri, Path)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }
  // #hello_example
  test("akka location") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val Prefix = "prefix"

    val actorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-1"
    )

    val registrationResult = locationService.register(AkkaRegistration(connection, actorRef, Prefix)).await

    registrationResult.componentId shouldBe componentId

    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))
    val uri = new URI(actorPath.toString)

    locationService.list.await shouldBe List(
      ResolvedAkkaLocation(connection, uri, Prefix, Some(actorRef))
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("tracking") {

    val reg = TcpRegistration(TcpConnection(ComponentId("redis2", ComponentType.Service)), 1234)
    val reg2 = TcpRegistration(TcpConnection(ComponentId("redis3", ComponentType.Service)), 1111)

    import actorRuntime._

    val source = locationService.track(reg.connection)

    val (switch, probe) = source.toMat(TestSink.probe[Location])(Keep.both).run()

    val registrationResult2 = locationService.register(reg2).await
    registrationResult2.unregister().await

    val registrationResult = locationService.register(reg).await
    registrationResult.unregister().await

    val uri = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:${reg.port}")

    probe
      .request(3)
      .expectNext(Unresolved(reg.connection))
      .expectNext(ResolvedTcpLocation(reg.connection, uri))
      .expectNext(Removed(reg.connection))

    switch.shutdown()

    probe
      .request(1)
      .expectComplete()

  }

  test("Registration should validate unique name of service"){
    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val duplicateReg = TcpRegistration(connection, 1234)
    val reg = TcpRegistration(connection, 1111)

    locationService.register(reg).await

    val illegalStateException = intercept[IllegalStateException]{
      locationService.register(duplicateReg).await
    }
    illegalStateException.getMessage shouldBe (s"A service with name ${duplicateReg.connection.name} is already registered")

    locationService.unregister(connection).await
  }


  test ("Resolve tcp connection") {
    val connection = TcpConnection(ComponentId("redis5", ComponentType.Service))
    locationService.register(TcpRegistration(connection, 1234))

    val locations = locationService.list.await

    locations.foreach(println)

    val resolvedCon = locationService.resolve(connection).await

    resolvedCon.connection shouldBe connection
  }
}
