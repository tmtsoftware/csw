package csw.services.location.scaladsl

import java.net.URI

import akka.actor.{Actor, ActorPath, PoisonPill, Props}
import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class LocationServiceCompTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val actorRuntime = new ActorRuntime()
  val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

  override protected def afterEach(): Unit = {
    locationService.unregisterAll().await
  }

  override protected def afterAll(): Unit = {
    actorRuntime.terminate().await
  }

  test("tcp location") {
    val componentId: ComponentId = ComponentId("exampleTCPService", ComponentType.Service)
    val connection: TcpConnection = TcpConnection(componentId)
    val Port: Int = 1234

    val location = TcpRegistration(connection,  Port)

    val registrationResult = locationService.register(location).await

    locationService.resolve(connection).await.get shouldBe location.location(Networks.hostname())
    locationService.list.await shouldBe List(location.location(Networks.hostname()))

    registrationResult.unregister().await

    locationService.resolve(connection).await shouldBe None
    locationService.list.await shouldBe List.empty
  }

  test("http location") {
    val componentId: ComponentId = ComponentId("exampleHTTPService", ComponentType.Service)
    val httpConnection: HttpConnection = HttpConnection(componentId)
    val Port: Int = 8080
    val Path: String = "path/to/resource"

    val httpRegistration = HttpRegistration(httpConnection,  Port, Path)
    val registrationResult = locationService.register(httpRegistration).await
    registrationResult.location.connection shouldBe httpConnection

    locationService.list.await shouldBe List(httpRegistration.location(Networks.hostname()))

    registrationResult.unregister().await
    locationService.list.await shouldBe List.empty
  }

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
    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))

    val registrationResult = locationService.register(AkkaRegistration(connection, actorRef)).await

    registrationResult.location.connection shouldBe connection

    Thread.sleep(10)

    locationService.list.await shouldBe List(AkkaRegistration(connection, actorRef).location(Networks.hostname()))

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("akka location death watch actor should unregister services whose actorRef is terminated") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val Prefix = "prefix"

    val actorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-to-die"
    )
    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))

    val registrationResult = locationService.register(AkkaRegistration(connection, actorRef)).await

    registrationResult.location.connection shouldBe connection

    Thread.sleep(10)

    locationService.list.await shouldBe List(AkkaRegistration(connection, actorRef).location(Networks.hostname()))

    actorRef ! PoisonPill

    Thread.sleep(2000)

    locationService.list.await shouldBe List.empty
  }

  test("tracking") {
    import actorRuntime._

    val Port = 1234
    val redis1Connection = TcpConnection(ComponentId("redis1", ComponentType.Service))
    val redis1Location = TcpRegistration(redis1Connection,  Port)

    val redis2Connection = TcpConnection(ComponentId("redis2", ComponentType.Service))
    val redis2Location = TcpRegistration(redis2Connection,  Port)

    val (switch, probe) = locationService.track(redis1Connection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

    val result = locationService.register(redis1Location).await
    val result2 = locationService.register(redis2Location).await
    probe.request(1)
    probe.expectNext(LocationUpdated(redis1Location.location(Networks.hostname())))

    result.unregister().await
    result2.unregister().await
    probe.request(1)
    probe.expectNext(LocationRemoved(redis1Connection))

    switch.shutdown()
    probe.request(1)
    probe.expectComplete()
  }

  test("Can not register a different connection against already registered name"){
    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val duplicateLocation = TcpRegistration(connection,  1234)
    val location = TcpRegistration(connection,  1111)

    val result = locationService.register(location).await

    val illegalStateException1 = intercept[IllegalStateException]{
      locationService.register(duplicateLocation).await
    }

    result.unregister().await
  }

  test("registering an already registered connection does not result in failure"){
    val componentId = ComponentId("redis4", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val duplicateTcpRegistration = TcpRegistration(connection,  1234)
    val tcpRegistration = TcpRegistration(connection,  1234)

    val result = locationService.register(tcpRegistration).await

    val result2 = locationService.register(duplicateTcpRegistration).await

    result2.location.connection shouldBe connection

    result.unregister().await
    result2.unregister().await
  }

  test("unregistering an already unregistered connection does not result in failure"){
    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val location = TcpRegistration(connection,  1111)

    val result = locationService.register(location).await

    result.unregister().await
    result.unregister().await
  }

  test ("Resolve tcp connection") {
    val connection = TcpConnection(ComponentId("redis5", ComponentType.Service))
    locationService.register(TcpRegistration(connection,  1234)).await

    val resolvedCon = locationService.resolve(connection).await.get

    resolvedCon.connection shouldBe connection
  }

  test("Should filter components with component type") {
    val hcdConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-2"
    )

    locationService.register(AkkaRegistration(hcdConnection, actorRef)).await

    val redisConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(redisConnection,  1234)).await

    val configServiceConnection = TcpConnection(ComponentId("configservice", ComponentType.Service))
    locationService.register(TcpRegistration(configServiceConnection,  1234)).await

    val filteredHCDs = locationService.list(ComponentType.HCD).await

    filteredHCDs.map(_.connection) shouldBe List(hcdConnection)

    val filteredServices = locationService.list(ComponentType.Service).await

    filteredServices.map(_.connection).toSet shouldBe Set(redisConnection, configServiceConnection)
  }

  test("should filter connections based on Connection type") {
    val hcdAkkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-3"
    )


    locationService.register(AkkaRegistration(hcdAkkaConnection, actorRef)).await

    val redisTcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(redisTcpConnection,  1234)).await

    val configTcpConnection = TcpConnection(ComponentId("configservice", ComponentType.Service))
    locationService.register(TcpRegistration(configTcpConnection,  1234)).await

    val assemblyHttpConnection = HttpConnection(ComponentId("assembly1", ComponentType.Assembly))
    val registrationResult = locationService.register(HttpRegistration(assemblyHttpConnection,  1234, "path123")).await

    val tcpConnections = locationService.list(ConnectionType.TcpType).await
    tcpConnections.map(_.connection).toSet shouldBe Set(redisTcpConnection, configTcpConnection)

    val httpConnections = locationService.list(ConnectionType.HttpType).await
    httpConnections.map(_.connection).toSet shouldBe Set(assemblyHttpConnection)

    val akkaConnections = locationService.list(ConnectionType.AkkaType).await
    akkaConnections.map(_.connection).toSet shouldBe Set(hcdAkkaConnection)
  }

  test("should filter connections based on hostname") {
    val tcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(tcpConnection,  1234)).await

    val httpConnection = HttpConnection(ComponentId("assembly1", ComponentType.Assembly))
    val registrationResult = locationService.register(HttpRegistration(httpConnection,  1234, "path123")).await

    val filteredLocations = locationService.list(Networks.hostname()).await

    filteredLocations.map(_.connection).toSet shouldBe Set(tcpConnection, httpConnection)

    locationService.list("Invalid_hostname").await shouldBe List.empty
  }
}
