package csw.services.location.scaladsl.demo

import akka.actor.{Actor, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit.TestProbe
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorSystemFactory, LocationServiceFactory}
import org.scalatest._

import scala.async.Async._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class LocationServiceDemoExample extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private implicit
  //#create-actor-system
  val actorSystem = ActorSystemFactory.remote()
  //#create-actor-system

  import actorSystem.dispatcher
  implicit val mat = ActorMaterializer()
  lazy
  //#create-location-service
  val locationService = LocationServiceFactory.make()
  //#create-location-service

  override protected def afterAll(): Unit = {
    locationService.shutdown().await
    actorSystem.terminate().await
  }

  //#Components-Connections-Registrations
  val tcpConnection   = TcpConnection(ComponentId("redis", ComponentType.Service))
  val tcpRegistration = TcpRegistration(tcpConnection, 6380)

  val httpConnection   = HttpConnection(ComponentId("configuration", ComponentType.Service))
  val httpRegistration = HttpRegistration(httpConnection, 8080, "path123")

  val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  val akkaRegistration = AkkaRegistration(akkaConnection,
    actorSystem.actorOf(
      Props(new Actor {
      override def receive: Receive = {
        case "print" => println("hello world")
      }
    }),
      "my-actor-1"
    ))
  //#Components-Connections-Registrations

  test("demo") {
    val assertionF: Future[Assertion] =
      //#register-list-resolve-unregister
      async {
        val tcpRegistrationResult = await(locationService.register(tcpRegistration))

        tcpRegistrationResult.location.connection shouldBe tcpConnection

        await(locationService.list) shouldBe List(tcpRegistrationResult.location)
        await(locationService.resolve(tcpConnection, 5.seconds)) shouldBe Some(tcpRegistrationResult.location)

        println(tcpRegistrationResult.location.uri)

        await(tcpRegistrationResult.unregister())

        await(locationService.list) shouldBe List.empty
        await(locationService.find(tcpConnection)) shouldBe None
      }
    Await.result(assertionF, 5.seconds)
    //#register-list-resolve-unregister
  }

  test("tracking") {
    //#tracking
    val (killSwitch, doneF) = locationService.track(tcpConnection).toMat(Sink.foreach(println))(Keep.both).run()

    Thread.sleep(200)

    async {
      val tcpRegistrationResult = await(locationService.register(tcpRegistration))
      await(locationService.register(httpRegistration))

      Thread.sleep(200)

      await(tcpRegistrationResult.unregister())
      await(locationService.unregister(httpConnection))

      Thread.sleep(200)
      killSwitch.shutdown()
    }

    Await.result(doneF, 5.seconds)
    //#tracking
  }

  test("filtering") {
    //#filtering
    val assertionF: Future[Assertion] = async {
      val tcpRegistrationResult  = await(locationService.register(tcpRegistration))
      val httpRegistrationResult = await(locationService.register(httpRegistration))
      val akkaRegistrationResult = await(locationService.register(akkaRegistration))

      await(locationService.list).toSet shouldBe Set(tcpRegistrationResult.location, httpRegistrationResult.location,
        akkaRegistrationResult.location)
      await(locationService.list(ConnectionType.AkkaType)).toSet shouldBe Set(akkaRegistrationResult.location)
      await(locationService.list(ComponentType.Service)).toSet shouldBe Set(tcpRegistrationResult.location,
        httpRegistrationResult.location)
      await(locationService.list(new Networks().hostname())).toSet shouldBe Set(tcpRegistrationResult.location,
        httpRegistrationResult.location, akkaRegistrationResult.location)
    }

    Await.result(assertionF, 5.seconds)
    //#filtering
  }

  test("subscribing") {
    //#subscribing
    val hostname = new Networks().hostname()

    //Test probe actor to receive the TrackingEvent notifications
    val probeActorRef = TestProbe()

    val switch = locationService.subscribe(tcpConnection, trackingEvent => probeActorRef.ref ! trackingEvent)

    val assertionF = async {
      await(locationService.register(tcpRegistration))
      probeActorRef.expectMsg(LocationUpdated(tcpRegistration.location(hostname)))

      await(locationService.unregister(tcpConnection))
      probeActorRef.expectMsg(LocationRemoved(tcpRegistration.connection))

      //shutdown the notification stream, should no longer receive any notifications
      switch.shutdown()

      await(locationService.register(tcpRegistration))
      probeActorRef.expectNoMsg()
    }

    Await.result(assertionF, 10.seconds)
    //#subscribing
  }

  test("shutdown") {
    //#shutdown
    Await.result(locationService.shutdown(), 20.seconds)
    //#shutdown
  }
}
