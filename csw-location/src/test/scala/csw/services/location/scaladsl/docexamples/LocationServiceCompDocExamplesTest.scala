package csw.services.location.scaladsl

import akka.actor.{Actor, ActorRef, Props}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Future

class LocationServiceCompDocExamplesTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  test("tcp location") {
    //#register_tcp_connection
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    //To register a tcp endpoint on host tcp://10.1.2.22:1234
    val Port: Int = 1234
    val componentId: ComponentId = ComponentId("exampleTCPService", ComponentType.Service)
    val connection: TcpConnection = TcpConnection(componentId)

    val location = TcpRegistration(connection,  Port)

    val result: Future[RegistrationResult] = locationService.register(location)
    //#register_tcp_connection

    val registrationResult = result.await

    locationService.resolve(connection).await.get shouldBe location.location(Networks.hostname())
    locationService.list.await shouldBe List(location.location(Networks.hostname()))

    registrationResult.unregister().await

    locationService.resolve(connection).await shouldBe None
    locationService.list.await shouldBe List.empty

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("http location") {
    //#register_http_connection
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    //To register a http endpoint on host http://10.1.2.22:8080/path/to/resource
    val Port: Int = 8080
    val Path: String = "path/to/resource"

    val componentId: ComponentId = ComponentId("exampleHTTPService", ComponentType.Service)
    val httpConnection: HttpConnection = HttpConnection(componentId)

    val resolvedHttpLocation = HttpRegistration(httpConnection,  Port, Path)

    val registrationResultF: Future[RegistrationResult] = locationService.register(resolvedHttpLocation)
    //#register_http_connection
    val registrationResult = registrationResultF.await
    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(resolvedHttpLocation.location(Networks.hostname()))

    registrationResult.unregister().await
    locationService.list.await shouldBe List.empty

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("akka location") {
    //#register_akka_connection
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    val componentId: ComponentId = ComponentId("tromboneHcd", ComponentType.HCD)
    val connection: AkkaConnection = AkkaConnection(componentId)

    val actorRef: ActorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-1"
    )
    val akkaLocation = AkkaRegistration(connection, actorRef)

    val registrationResultF: Future[RegistrationResult] = locationService.register(akkaLocation)
    //#register_akka_connection

    registrationResultF.await.componentId shouldBe componentId

    Thread.sleep(10)

    locationService.list.await shouldBe List(akkaLocation.location(Networks.hostname()))

    registrationResultF.await.unregister().await

    locationService.list.await shouldBe List.empty

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("tracking") {
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)
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

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("Can not register a different connection against already registered name"){
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val duplicateLocation = TcpRegistration(connection,  1234)
    val location = TcpRegistration(connection,  1111)

    val result = locationService.register(location).await

    val illegalStateException1 = intercept[IllegalStateException]{
      locationService.register(duplicateLocation).await
    }

    illegalStateException1.getMessage shouldBe s"there is other location=${location.location(Networks.hostname())} registered against name=${duplicateLocation.connection.name}."

    result.unregister().await

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("registering an already registered connection does not result in failure"){
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    val componentId = ComponentId("redis4", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val duplicateLocation = TcpRegistration(connection,  1234)
    val location = TcpRegistration(connection,  1234)

    val result = locationService.register(location).await

    val result2 = locationService.register(duplicateLocation).await

    result2.componentId shouldBe componentId

    result.unregister().await
    result2.unregister().await

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("unregistering an already unregistered connection does not result in failure"){
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val location = TcpRegistration(connection,  1111)

    val result = locationService.register(location).await

    result.unregister().await
    result.unregister().await

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test ("Resolve tcp connection") {
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    val connection = TcpConnection(ComponentId("redis5", ComponentType.Service))
    locationService.register(TcpRegistration(connection,  1234)).await

    val resolvedCon = locationService.resolve(connection).await.get

    resolvedCon.connection shouldBe connection

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("Should filter components with component type") {
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

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

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("should filter connections based on Connection type") {
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

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

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }

  test("should filter connections based on hostname") {
    val actorRuntime = new ActorRuntime()
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

    val tcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(tcpConnection,  1234)).await

    val httpConnection = HttpConnection(ComponentId("assembly1", ComponentType.Assembly))
    val registrationResult = locationService.register(HttpRegistration(httpConnection,  1234, "path123")).await

    val filteredLocations = locationService.list(Networks.hostname()).await

    filteredLocations.map(_.connection).toSet shouldBe Set(tcpConnection, httpConnection)

    locationService.list("Invalid_hostname").await shouldBe List.empty

    locationService.unregisterAll().await
    actorRuntime.terminate().await
  }
}
