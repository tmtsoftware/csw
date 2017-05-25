package csw.services.location.scaladsl

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.exceptions.OtherLocationIsRegistered
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

class LocationServiceCompTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  lazy val locationService: LocationService = LocationServiceFactory.make()

  implicit val actorSystem: ActorSystem = ActorSystemFactory.remote("test")
  implicit val mat: Materializer        = ActorMaterializer()

  override protected def afterEach(): Unit =
    locationService.unregisterAll().await

  override protected def afterAll(): Unit = {
    locationService.shutdown().await
    actorSystem.terminate().await
  }

  test("should able to register, resolve, list and unregister tcp location") {
    val componentId: ComponentId         = ComponentId("exampleTCPService", ComponentType.Service)
    val connection: TcpConnection        = TcpConnection(componentId)
    val Port: Int                        = 1234
    val tcpRegistration: TcpRegistration = TcpRegistration(connection, Port)

    // register, resolve & list tcp connection for the first time
    locationService.register(tcpRegistration).await
    locationService.resolve(connection, 2.seconds).await.get shouldBe tcpRegistration.location(
        new Networks().hostname())
    locationService.list.await shouldBe List(tcpRegistration.location(new Networks().hostname()))

    // unregister, resolve & list tcp connection
    locationService.unregister(connection).await
    locationService.resolve(connection, 2.seconds).await shouldBe None
    locationService.list.await shouldBe List.empty

    // re-register, resolve & list tcp connection
    locationService.register(tcpRegistration).await
    locationService.resolve(connection, 2.seconds).await.get shouldBe tcpRegistration.location(
        new Networks().hostname())
    locationService.list.await shouldBe List(tcpRegistration.location(new Networks().hostname()))
  }

  test("should able to register, resolve, list and unregister http location") {
    val componentId: ComponentId           = ComponentId("exampleHTTPService", ComponentType.Service)
    val httpConnection: HttpConnection     = HttpConnection(componentId)
    val Port: Int                          = 8080
    val Path: String                       = "path/to/resource"
    val httpRegistration: HttpRegistration = HttpRegistration(httpConnection, Port, Path)

    // register, resolve & list http connection for the first time
    locationService.register(httpRegistration).await.location.connection shouldBe httpConnection
    locationService.resolve(httpConnection, 2.seconds).await.get shouldBe httpRegistration.location(
        new Networks().hostname())
    locationService.list.await shouldBe List(httpRegistration.location(new Networks().hostname()))

    // unregister, resolve & list http connection
    locationService.unregister(httpConnection).await
    locationService.resolve(httpConnection, 2.seconds).await shouldBe None
    locationService.list.await shouldBe List.empty

    // re-register, resolve & list http connection
    locationService.register(httpRegistration).await.location.connection shouldBe httpConnection
    locationService.resolve(httpConnection, 2.seconds).await.get shouldBe httpRegistration.location(
        new Networks().hostname())
    locationService.list.await shouldBe List(httpRegistration.location(new Networks().hostname()))
  }

  test("should able to register, resolve, list and unregister akka location") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection  = AkkaConnection(componentId)
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-1"
    )
    val akkaRegistration = AkkaRegistration(connection, actorRef)

    // register, resolve & list akka connection for the first time
    locationService.register(akkaRegistration).await.location.connection shouldBe connection
    locationService.resolve(connection, 2.seconds).await.get shouldBe akkaRegistration.location(
        new Networks().hostname())
    locationService.list.await shouldBe List(akkaRegistration.location(new Networks().hostname()))

    // unregister, resolve & list akka connection
    locationService.unregister(connection).await
    locationService.resolve(connection, 2.seconds).await shouldBe None
    locationService.list.await shouldBe List.empty

    // re-register, resolve & list akka connection
    locationService.register(akkaRegistration).await.location.connection shouldBe connection
    locationService.resolve(connection, 2.seconds).await.get shouldBe akkaRegistration.location(
        new Networks().hostname())
    locationService.list.await shouldBe List(akkaRegistration.location(new Networks().hostname()))
  }

  test("akka location death watch actor should unregister services whose actorRef is terminated") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection  = AkkaConnection(componentId)

    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-to-die"
    )

    locationService.register(AkkaRegistration(connection, actorRef)).await.location.connection shouldBe connection

    Thread.sleep(10)

    locationService.list.await shouldBe List(AkkaRegistration(connection,
        actorRef).location(new Networks().hostname()))

    actorRef ! PoisonPill

    Thread.sleep(2000)

    locationService.list.await shouldBe List.empty
  }

  test(
      "should able to track tcp connection and get location updated(on registration) and remove(on unregistration) messages") {
    val Port               = 1234
    val redis1Connection   = TcpConnection(ComponentId("redis1", ComponentType.Service))
    val redis1Registration = TcpRegistration(redis1Connection, Port)

    val redis2Connection   = TcpConnection(ComponentId("redis2", ComponentType.Service))
    val redis2registration = TcpRegistration(redis2Connection, Port)

    val (switch, probe) = locationService.track(redis1Connection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

    val result  = locationService.register(redis1Registration).await
    val result2 = locationService.register(redis2registration).await
    probe.request(1)
    probe.expectNext(LocationUpdated(redis1Registration.location(new Networks().hostname())))

    result.unregister().await
    result2.unregister().await
    probe.request(1)
    probe.expectNext(LocationRemoved(redis1Connection))

    switch.shutdown()
    probe.request(1)
    probe.expectComplete()
  }

  test("should be able to subscribe a tcp connection and receive notifications via callback") {
    val hostname           = new Networks().hostname()
    val Port               = 1234
    val redis1Connection   = TcpConnection(ComponentId("redis1", ComponentType.Service))
    val redis1Registration = TcpRegistration(redis1Connection, Port)

    val probe = TestProbe()

    val switch = locationService.subscribe(redis1Connection, te => probe.ref ! te)

    locationService.register(redis1Registration).await
    probe.expectMsg(LocationUpdated(redis1Registration.location(hostname)))

    locationService.unregister(redis1Connection).await
    probe.expectMsg(LocationRemoved(redis1Registration.connection))

    switch.shutdown()
    locationService.register(redis1Registration).await
    probe.expectNoMsg()
  }

  test("should able to track http and akka connection registered before tracking started") {
    val hostname = new Networks().hostname()
    //create http registration
    val port             = 9595
    val prefix           = "/trombone/hcd"
    val httpConnection   = HttpConnection(new ComponentId("Assembly1", ComponentType.Assembly))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    //create akka registration
    val akkaComponentId = ComponentId("container1", ComponentType.Container)
    val akkaConnection  = AkkaConnection(akkaComponentId)
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "container1-actor"
    )
    val akkaRegistration = AkkaRegistration(akkaConnection, actorRef)

    val httpRegistrationResult = locationService.register(httpRegistration).await
    val akkaRegistrationResult = locationService.register(akkaRegistration).await

    //start tracking both http and akka connections
    val (httpSwitch, httpProbe) =
      locationService.track(httpConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
    val (akkaSwitch, akkaProbe) =
      locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

    httpProbe.request(1)
    httpProbe.expectNext(LocationUpdated(httpRegistration.location(hostname)))

    akkaProbe.request(1)
    akkaProbe.expectNext(LocationUpdated(akkaRegistration.location(hostname)))

    //unregister http connection
    httpRegistrationResult.unregister().await
    httpProbe.request(1)
    httpProbe.expectNext(LocationRemoved(httpConnection))

    //stop tracking http connection
    httpSwitch.shutdown()
    httpProbe.request(1)
    httpProbe.expectComplete()

    //unregister and stop tracking akka connection
    akkaRegistrationResult.unregister().await
    akkaProbe.request(1)
    akkaProbe.expectNext(LocationRemoved(akkaConnection))

    akkaSwitch.shutdown()
    akkaProbe.request(1)
    akkaProbe.expectComplete()
  }

  test("should able to stop tracking") {
    val hostname = new Networks().hostname()
    //create http registration
    val port             = 9595
    val prefix           = "/trombone/hcd"
    val httpConnection   = HttpConnection(ComponentId("trombone1", ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val httpRegistrationResult = locationService.register(httpRegistration).await

    //start tracking http connection
    val (httpSwitch, httpProbe) =
      locationService.track(httpConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

    httpProbe.request(1)
    httpProbe.expectNext(LocationUpdated(httpRegistration.location(hostname)))

    //stop tracking http connection
    httpSwitch.shutdown()
    httpProbe.request(1)
    httpProbe.expectComplete()

    httpRegistrationResult.unregister().await

    httpProbe.expectNoMsg()
  }

  test("should not register a different Registration(connection + port/URI/actorRef) against already registered name") {
    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val duplicateTcpRegistration = TcpRegistration(connection, 1234)
    val tcpRegistration          = TcpRegistration(connection, 1111)

    val result = locationService.register(tcpRegistration).await

    intercept[OtherLocationIsRegistered] {
      locationService.register(duplicateTcpRegistration).await
    }

    result.unregister().await
  }

  test(
      "registering an already registered Registration(connection + port/URI/actorRef) on same machine should not result in failure") {
    val componentId = ComponentId("redis4", ComponentType.Service)
    val connection  = TcpConnection(componentId)

    val duplicateTcpRegistration = TcpRegistration(connection, 1234)
    val tcpRegistration          = TcpRegistration(connection, 1234)

    val result = locationService.register(tcpRegistration).await

    val result2 = locationService.register(duplicateTcpRegistration).await

    result.location.connection shouldBe connection
    result2.location.connection shouldBe connection

    result.unregister().await
    // Location service should be empty because second register above did nothing -- you can only register once
    locationService.list.await shouldBe List.empty

    // Second unregister does nothing and does not produce an error
    result2.unregister().await
  }

  test("unregistering an already unregistered connection does not result in failure") {
    val connection = TcpConnection(ComponentId("redis4", ComponentType.Service))

    val tcpRegistration = TcpRegistration(connection, 1111)

    val result = locationService.register(tcpRegistration).await

    result.unregister().await
    result.unregister().await
  }

  test("should able to resolve tcp connection") {
    val connection = TcpConnection(ComponentId("redis5", ComponentType.Service))
    locationService.register(TcpRegistration(connection, 1234)).await

    locationService.find(connection).await.get.connection shouldBe connection
  }

  test("should filter components with component type") {
    val hcdConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-2"
    )

    locationService.register(AkkaRegistration(hcdConnection, actorRef)).await

    val redisConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(redisConnection, 1234)).await

    val configServiceConnection = TcpConnection(ComponentId("configservice", ComponentType.Service))
    locationService.register(TcpRegistration(configServiceConnection, 1234)).await

    locationService.list(ComponentType.HCD).await.map(_.connection) shouldBe List(hcdConnection)

    val filteredServices = locationService.list(ComponentType.Service).await

    filteredServices.map(_.connection).toSet shouldBe Set(redisConnection, configServiceConnection)
  }

  test("should filter connections based on Connection type") {
    val hcdAkkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-3"
    )

    locationService.register(AkkaRegistration(hcdAkkaConnection, actorRef)).await

    val redisTcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(redisTcpConnection, 1234)).await

    val configTcpConnection = TcpConnection(ComponentId("configservice", ComponentType.Service))
    locationService.register(TcpRegistration(configTcpConnection, 1234)).await

    val assemblyHttpConnection = HttpConnection(ComponentId("assembly1", ComponentType.Assembly))
    locationService.register(HttpRegistration(assemblyHttpConnection, 1234, "path123")).await

    locationService.list(ConnectionType.TcpType).await.map(_.connection).toSet shouldBe Set(redisTcpConnection,
      configTcpConnection)

    val httpConnections = locationService.list(ConnectionType.HttpType).await
    httpConnections.map(_.connection).toSet shouldBe Set(assemblyHttpConnection)

    val akkaConnections = locationService.list(ConnectionType.AkkaType).await
    akkaConnections.map(_.connection).toSet shouldBe Set(hcdAkkaConnection)
  }

  test("should filter connections based on hostname") {
    val tcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    locationService.register(TcpRegistration(tcpConnection, 1234)).await

    val httpConnection = HttpConnection(ComponentId("assembly1", ComponentType.Assembly))
    locationService.register(HttpRegistration(httpConnection, 1234, "path123")).await

    val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-4"
    )
    locationService.register(AkkaRegistration(akkaConnection, actorRef)).await

    locationService.list(new Networks().hostname()).await.map(_.connection).toSet shouldBe Set(tcpConnection,
      httpConnection, akkaConnection)

    locationService.list("Invalid_hostname").await shouldBe List.empty
  }

  test("should able to unregister all components") {
    val Port               = 1234
    val redis1Connection   = TcpConnection(ComponentId("redis1", ComponentType.Service))
    val redis1Registration = TcpRegistration(redis1Connection, Port)

    val redis2Connection   = TcpConnection(ComponentId("redis2", ComponentType.Service))
    val redis2registration = TcpRegistration(redis2Connection, Port)

    locationService.register(redis1Registration).await
    locationService.register(redis2registration).await

    locationService.list.await.map(_.connection).toSet shouldBe Set(redis1Connection, redis2Connection)
    locationService.unregisterAll().await
    locationService.list.await shouldBe List.empty
  }
}
