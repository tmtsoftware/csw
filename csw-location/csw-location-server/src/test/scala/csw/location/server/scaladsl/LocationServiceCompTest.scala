package csw.location.server.scaladsl

import akka.actor.testkit.typed.scaladsl
import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{Behavior, SpawnProtocol}
import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit.TestProbe
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models.ComponentType.{Assembly, HCD, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService
import csw.location.api.{AkkaRegistrationFactory, models}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.LocationServiceFactory
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.network.utils.Networks
import csw.prefix.models.{Prefix, Subsystem}
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class LocationServiceCompTestWithCluster extends LocationServiceCompTest("cluster")

class LocationServiceCompTest(mode: String)
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Eventually {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  implicit private val typedSystem: typed.ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "test")
  lazy private val clusterSettings                                           = ClusterAwareSettings
  lazy private val clusterSystem: typed.ActorSystem[SpawnProtocol.Command]   = clusterSettings.system
  implicit val ec: ExecutionContext                                          = typedSystem.executionContext
  private lazy val locationService: LocationService = mode match {
    case "http"    => HttpLocationServiceFactory.makeLocalClient
    case "cluster" => LocationServiceFactory.make(ClusterAwareSettings)
  }
  import AkkaRegistrationFactory._

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override protected def afterEach(): Unit = locationService.unregisterAll().await

  override protected def afterAll(): Unit = {
    if (mode == "cluster") {
      clusterSystem.terminate()
      clusterSystem.whenTerminated.await
    }
    typedSystem.terminate()
    typedSystem.whenTerminated.await
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-16: Register a component
  // DEOPSCSW-17: List registered services
  // DEOPSCSW-20: Register a service
  // DEOPSCSW-23: Unregister a comp/service
  // DEOPSCSW-34: Resolve a connection
  test(
    "should able to register, resolve, list and unregister tcp location| DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val componentId: ComponentId         = models.ComponentId(Prefix(Subsystem.CSW, "exampleTCPService"), ComponentType.Service)
    val connection: TcpConnection        = TcpConnection(componentId)
    val Port: Int                        = 1234
    val tcpRegistration: TcpRegistration = TcpRegistration(connection, Port)

    // register, resolve & list tcp connection for the first time
    locationService.register(tcpRegistration).await
    locationService.resolve(connection, 2.seconds).await.get shouldBe tcpRegistration.location(Networks().hostname)
    locationService.list.await shouldBe List(tcpRegistration.location(Networks().hostname))

    // unregister, resolve & list tcp connection
    locationService.unregister(connection).await
    locationService.resolve(connection, 2.seconds).await shouldBe None
    locationService.list.await shouldBe List.empty

    // re-register, resolve & list tcp connection
    locationService.register(tcpRegistration).await
    locationService.resolve(connection, 2.seconds).await.get shouldBe tcpRegistration.location(
      Networks().hostname
    )
    locationService.list.await shouldBe List(tcpRegistration.location(Networks().hostname))
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-16: Register a component
  // DEOPSCSW-17: List registered services
  // DEOPSCSW-23: Unregister a comp/service
  // DEOPSCSW-34: Resolve a connection
  test(
    "should able to register, resolve, list and unregister http location | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val componentId: ComponentId           = models.ComponentId(Prefix(Subsystem.CSW, "exampleHTTPService"), ComponentType.Service)
    val httpConnection: HttpConnection     = HttpConnection(componentId)
    val Port: Int                          = 8080
    val Path: String                       = "path/to/resource"
    val httpRegistration: HttpRegistration = HttpRegistration(httpConnection, Port, Path)

    // register, resolve & list http connection for the first time
    locationService.register(httpRegistration).await.location.connection shouldBe httpConnection
    locationService.resolve(httpConnection, 2.seconds).await.get shouldBe httpRegistration.location(
      Networks().hostname
    )
    locationService.list.await shouldBe List(httpRegistration.location(Networks().hostname))

    // unregister, resolve & list http connection
    locationService.unregister(httpConnection).await
    locationService.resolve(httpConnection, 2.seconds).await shouldBe None
    locationService.list.await shouldBe List.empty

    // re-register, resolve & list http connection
    locationService.register(httpRegistration).await.location.connection shouldBe httpConnection
    locationService.resolve(httpConnection, 2.seconds).await.get shouldBe httpRegistration.location(
      Networks().hostname
    )
    locationService.list.await shouldBe List(httpRegistration.location(Networks().hostname))
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-16: Register a component
  // DEOPSCSW-17: List registered services
  // DEOPSCSW-23: Unregister a comp/service
  // DEOPSCSW-34: Resolve a connection
  test(
    "should able to register, resolve, list and unregister akka location | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val componentId      = models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), HCD)
    val connection       = AkkaConnection(componentId)
    val actorRef         = typedSystem.spawn(Behaviors.empty, "my-actor-1")
    val akkaRegistration = AkkaRegistrationFactory.make(connection, actorRef)

    // register, resolve & list akka connection for the first time
    locationService.register(akkaRegistration).await.location.connection shouldBe connection
    locationService.resolve(connection, 2.seconds).await.get shouldBe akkaRegistration.location(
      Networks().hostname
    )
    locationService.list.await shouldBe List(akkaRegistration.location(Networks().hostname))

    // unregister, resolve & list akka connection
    locationService.unregister(connection).await
    locationService.resolve(connection, 2.seconds).await shouldBe None
    locationService.list.await shouldBe List.empty

    // re-register, resolve & list akka connection
    locationService.register(akkaRegistration).await.location.connection shouldBe connection
    locationService.resolve(connection, 2.seconds).await.get shouldBe akkaRegistration.location(
      Networks().hostname
    )
    locationService.list.await shouldBe List(akkaRegistration.location(Networks().hostname))
  }

  List(HCD, Assembly, Sequencer).foreach { compType =>
    test(
      s"unregistering $compType should unregister both Akka and Http connection | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
    ) {
      val prefix           = Prefix(Subsystem.NFIRAOS, "name")
      val akkaConnection   = AkkaConnection(models.ComponentId(prefix, compType))
      val httpConnection   = HttpConnection(models.ComponentId(prefix, compType))
      val actorRef         = typedSystem.spawn(Behaviors.empty, "my-actor-1")
      val akkaRegistration = AkkaRegistrationFactory.make(akkaConnection, actorRef)
      val httpRegistration = HttpRegistration(httpConnection, 8088, "/")

      locationService.register(akkaRegistration).await.location.connection shouldBe akkaConnection
      locationService.register(httpRegistration).await.location.connection shouldBe httpConnection

      locationService.unregister(akkaConnection).await
      locationService.list.await shouldBe empty
    }
  }

  // DEOPSCSW-23: Unregister a comp/service
  // DEOPSCSW-35: CRDT detects comp/service crash
  // DEOPSCSW-36: Track a crashed service/comp
  test(
    "akka location death watch actor should unregister services whose actorRef is terminated | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-35, DEOPSCSW-36, DEOPSCSW-429"
  ) {
    val componentId = models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), HCD)
    val connection  = AkkaConnection(componentId)

    val emptyBeh: Behavior[String] = Behaviors.receiveMessage {
      case "Kill" => Behaviors.stopped
      case _      => Behaviors.same
    }

    val actorRef = typedSystem.spawn(emptyBeh, "my-actor-to-die")

    locationService
      .register(make(connection, actorRef))
      .await
      .location
      .connection shouldBe connection

    locationService.list.await shouldBe List(make(connection, actorRef).location(Networks().hostname))

    actorRef ! "Kill"

    eventually(locationService.list.await shouldBe List.empty)
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-26: Track a connection
  // DEOPSCSW-36: Track a crashed service/comp
  test(
    "should able to track tcp connection and get location updated(on registration) and remove(on unregistration) messages | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-26, DEOPSCSW-36, DEOPSCSW-429"
  ) {
    val Port               = 1234
    val redis1Connection   = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis1"), ComponentType.Service))
    val redis1Registration = TcpRegistration(redis1Connection, Port)

    val redis2Connection   = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis2"), ComponentType.Service))
    val redis2registration = TcpRegistration(redis2Connection, Port)

    val probe = scaladsl.TestProbe[TrackingEvent]("test-probe")

    val switch = locationService.track(redis1Connection).toMat(Sink.foreach(probe.ref.tell(_)))(Keep.left).run()

    val result  = locationService.register(redis1Registration).await
    val result2 = locationService.register(redis2registration).await
    probe.expectMessage(LocationUpdated(redis1Registration.location(Networks().hostname)))

    result.unregister().await
    result2.unregister().await
    probe.expectMessage(LocationRemoved(redis1Connection))

    switch.cancel()
  }

  // DEOPSCSW-12: Create location service API
  test(
    "should be able to subscribe a tcp connection and receive notifications via callback | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val hostname           = Networks().hostname
    val Port               = 1234
    val redis1Connection   = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis1"), ComponentType.Service))
    val redis1Registration = TcpRegistration(redis1Connection, Port)

    val probe = TestProbe()(typedSystem.toClassic)

    val switch = locationService.subscribe(redis1Connection, te => probe.ref ! te)

    locationService.register(redis1Registration).await
    probe.expectMsg(LocationUpdated(redis1Registration.location(hostname)))

    locationService.unregister(redis1Connection).await
    probe.expectMsg(LocationRemoved(redis1Registration.connection))

    switch.cancel()
    locationService.register(redis1Registration).await
    probe.expectNoMessage(200.millis)
  }

  // DEOPSCSW-26: Track a connection  // TODO
  test(
    "should able to track http and akka connection registered before tracking started | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-26, DEOPSCSW-429"
  ) {
    val hostname = Networks().hostname
    //create http registration
    val port             = 9595
    val prefix           = Prefix(Subsystem.CSW, "Assembly1")
    val httpConnection   = HttpConnection(models.ComponentId(prefix, ComponentType.Assembly))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix.toString)

    //create akka registration
    val akkaComponentId  = models.ComponentId(Prefix(Subsystem.CSW, "container1"), ComponentType.Container)
    val akkaConnection   = AkkaConnection(akkaComponentId)
    val actorRef         = typedSystem.spawn(Behaviors.empty, "container1-actor")
    val akkaRegistration = AkkaRegistrationFactory.make(akkaConnection, actorRef)

    val httpRegistrationResult = locationService.register(httpRegistration).await
    val akkaRegistrationResult = locationService.register(akkaRegistration).await
    val httpProbe              = scaladsl.TestProbe[TrackingEvent]("http-probe")
    val akkaProbe              = scaladsl.TestProbe[TrackingEvent]("akka-probe")

    //start tracking both http and akka connections
    val httpSwitch = locationService.track(httpConnection).toMat(Sink.foreach(httpProbe.ref.tell(_)))(Keep.left).run()
    val akkaSwitch = locationService.track(akkaConnection).toMat(Sink.foreach(akkaProbe.ref.tell(_)))(Keep.left).run()

    httpProbe.expectMessage(LocationUpdated(httpRegistration.location(hostname)))
    akkaProbe.expectMessage(LocationUpdated(akkaRegistration.location(hostname)))

    //unregister http connection
    httpRegistrationResult.unregister().await
    httpProbe.expectMessage(LocationRemoved(httpConnection))

    //stop tracking http connection
    httpSwitch.cancel()

    //unregister and stop tracking akka connection
    akkaRegistrationResult.unregister().await
    akkaProbe.expectMessage(LocationRemoved(akkaConnection))

    akkaSwitch.cancel()
  }

  // DEOPSCSW-26: Track a connection
  test(
    "should able to stop tracking | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-26, DEOPSCSW-429"
  ) {
    val hostname = Networks().hostname
    //create http registration
    val port             = 9595
    val prefix           = Prefix(Subsystem.CSW, "trombone1")
    val httpConnection   = HttpConnection(models.ComponentId(prefix, HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix.toString)

    val httpRegistrationResult = locationService.register(httpRegistration).await

    val httpProbe = scaladsl.TestProbe[TrackingEvent]("test-probe")

    //start tracking http connection
    val httpSwitch = locationService.track(httpConnection).toMat(Sink.foreach(httpProbe.ref.tell(_)))(Keep.left).run()

    httpProbe.expectMessage(LocationUpdated(httpRegistration.location(hostname)))

    //stop tracking http connection
    httpSwitch.cancel()

    httpRegistrationResult.unregister().await

    httpProbe.expectNoMessage(200.millis)
  }

  // DEOPSCSW-16: Register a component
  // DEOPSCSW-18: Validate unique registrations
  test(
    "should not register a different Registration(connection + port/URI/actorRef) against already registered name | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-18, DEOPSCSW-429"
  ) {
    val connection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis4"), ComponentType.Service))

    val duplicateTcpRegistration = TcpRegistration(connection, 1234)
    val tcpRegistration          = TcpRegistration(connection, 1111)

    val result = locationService.register(tcpRegistration).await

    intercept[OtherLocationIsRegistered] {
      locationService.register(duplicateTcpRegistration).await
    }

    result.unregister().await
  }

  // DEOPSCSW-16: Register a component
  // DEOPSCSW-18: Validate unique registrations
  test(
    "registering an already registered Registration(connection + port/URI/actorRef) on same machine should not result in failure | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-18, DEOPSCSW-429"
  ) {
    val componentId = models.ComponentId(Prefix(Subsystem.CSW, "redis4"), ComponentType.Service)
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

  // DEOPSCSW-23: Unregister a comp/service
  test(
    "unregistering an already unregistered connection does not result in failure | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val connection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis4"), ComponentType.Service))

    val tcpRegistration = TcpRegistration(connection, 1111)

    val result = locationService.register(tcpRegistration).await

    result.unregister().await
    result.unregister().await
  }

  // DEOPSCSW-34: Resolve a connection
  test(
    "should able to resolve tcp connection | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val connection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis5"), ComponentType.Service))
    locationService.register(TcpRegistration(connection, 1234)).await

    locationService.find(connection).await.get.connection shouldBe connection
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-24: Filter by comp/service type
  test(
    "should filter components with component type | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-24, DEOPSCSW-429"
  ) {
    val hcdConnection = AkkaConnection(models.ComponentId(Prefix(Subsystem.CSW, "hcd1"), HCD))
    val actorRef      = typedSystem.spawn(Behaviors.empty, "my-actor-2")

    locationService.register(make(hcdConnection, actorRef)).await

    val redisConnection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))
    locationService.register(TcpRegistration(redisConnection, 1234)).await

    val configServiceConnection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "configservice"), ComponentType.Service))
    locationService.register(TcpRegistration(configServiceConnection, 1234)).await

    locationService.list(HCD).await.map(_.connection) shouldBe List(hcdConnection)

    val filteredServices = locationService.list(ComponentType.Service).await

    filteredServices.map(_.connection).toSet shouldBe Set(redisConnection, configServiceConnection)
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-32: Filter by connection type
  test(
    "should filter connections based on Connection type | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-32, DEOPSCSW-429"
  ) {
    val hcdAkkaConnection = AkkaConnection(models.ComponentId(Prefix(Subsystem.CSW, "hcd1"), HCD))
    val actorRef = typedSystem.spawn(
      Behaviors.empty,
      "my-actor-3"
    )

    locationService.register(make(hcdAkkaConnection, actorRef)).await

    val redisTcpConnection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))
    locationService.register(TcpRegistration(redisTcpConnection, 1234)).await

    val configTcpConnection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "configservice"), ComponentType.Service))
    locationService.register(TcpRegistration(configTcpConnection, 1234)).await

    val assemblyHttpConnection = HttpConnection(models.ComponentId(Prefix(Subsystem.CSW, "assembly1"), ComponentType.Assembly))
    locationService.register(HttpRegistration(assemblyHttpConnection, 1234, "path123")).await

    locationService.list(ConnectionType.TcpType).await.map(_.connection).toSet shouldBe Set(
      redisTcpConnection,
      configTcpConnection
    )

    val httpConnections = locationService.list(ConnectionType.HttpType).await
    httpConnections.map(_.connection).toSet shouldBe Set(assemblyHttpConnection)

    val akkaConnections = locationService.list(ConnectionType.AkkaType).await
    akkaConnections.map(_.connection).toSet shouldBe Set(hcdAkkaConnection)
  }

  // DEOPSCSW-12: Create location service API
  // DEOPSCSW-31: Filter by hostname
  test(
    "should filter connections based on hostname | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-31, DEOPSCSW-429"
  ) {
    val tcpConnection = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))
    locationService.register(TcpRegistration(tcpConnection, 1234)).await

    val httpConnection = HttpConnection(models.ComponentId(Prefix(Subsystem.CSW, "assembly1"), ComponentType.Assembly))
    locationService.register(HttpRegistration(httpConnection, 1234, "path123")).await

    val akkaConnection = AkkaConnection(models.ComponentId(Prefix(Subsystem.CSW, "hcd1"), HCD))
    val actorRef = typedSystem.spawn(
      Behaviors.empty,
      "my-actor-4"
    )

    locationService.register(make(akkaConnection, actorRef)).await

    locationService.list(Networks().hostname).await.map(_.connection).toSet shouldBe Set(
      tcpConnection,
      httpConnection,
      akkaConnection
    )

    locationService.list("Invalid_hostname").await shouldBe List.empty
  }

  // DEOPSCSW-308: Add prefix in Location service models
  // DEOPSCSW-12: Create location service API
  // CSW-86: Subsystem should be case-insensitive
  test(
    "should filter akka connections based on prefix | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-308, DEOPSCSW-429"
  ) {
    val akkaConnection1 = AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "ncc.trombone.hcd1"), HCD))
    val akkaConnection2 =
      AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "ncc.trombone.assembly2"), ComponentType.Assembly))
    val akkaConnection3 = AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "ncc.trombone.hcd3"), HCD))

    val actorRef  = typedSystem.spawn(Behaviors.empty, "")
    val actorRef2 = typedSystem.spawn(Behaviors.empty, "")
    val actorRef3 = typedSystem.spawn(Behaviors.empty, "")

    locationService.register(make(akkaConnection1, actorRef)).await
    locationService.register(make(akkaConnection2, actorRef2)).await
    locationService.register(make(akkaConnection3, actorRef3)).await

    locationService.listByPrefix("NFIRAOS.ncc.trombone").await.map(_.connection).toSet shouldBe Set(
      akkaConnection1,
      akkaConnection2,
      akkaConnection3
    )
  }

  // DEOPSCSW-12: Create location service API
  test(
    "should able to unregister all components | DEOPSCSW-12, DEOPSCSW-16, DEOPSCSW-17, DEOPSCSW-20, DEOPSCSW-23, DEOPSCSW-34, DEOPSCSW-429"
  ) {
    val Port               = 1234
    val redis1Connection   = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis1"), ComponentType.Service))
    val redis1Registration = TcpRegistration(redis1Connection, Port)

    val redis2Connection   = TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis2"), ComponentType.Service))
    val redis2registration = TcpRegistration(redis2Connection, Port)

    locationService.register(redis1Registration).await
    locationService.register(redis2registration).await

    locationService.list.await.map(_.connection).toSet shouldBe Set(redis1Connection, redis2Connection)
    locationService.unregisterAll().await
    locationService.list.await shouldBe List.empty
  }
}
