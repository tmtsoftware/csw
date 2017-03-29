package csw.services.location.scaladsl.demo

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest._

import scala.async.Async._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class LocationServiceDemoExample extends FunSuite with Matchers with BeforeAndAfterAll {

  private implicit val actorSystem = ActorSystem("demo")
  import actorSystem.dispatcher
  implicit val mat = ActorMaterializer()
  private val actorRef = actorSystem.actorOf(
    Props(new Actor {
      override def receive: Receive = {
        case "print" => println("hello world")
      }
    }),
    "my-actor-1"
  )


  //#create-location-service
  lazy val locationService = LocationServiceFactory.make()
  //#create-location-service


  override protected def afterAll(): Unit = {
    locationService.shutdown().await
  }

  //#Components-Connections-Registrations
  val tcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
  val tcpRegistration = TcpRegistration(tcpConnection, 6380)

  val httpConnection = HttpConnection(ComponentId("configuration", ComponentType.Service))
  val httpRegistration = HttpRegistration(httpConnection, 8080, "path123")

  val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  val akkaRegistration = AkkaRegistration(akkaConnection, actorRef)
  //#Components-Connections-Registrations

  test("demo") {
    val assertionF: Future[Assertion] =
      //#register-list-resolve-unregister
      async {
      val tcpRegistrationResult = await(locationService.register(tcpRegistration))

      tcpRegistrationResult.location.connection shouldBe tcpConnection

      await(locationService.list) shouldBe List(tcpRegistrationResult.location)
      await(locationService.resolve(tcpConnection)) shouldBe Some(tcpRegistrationResult.location)

      println(tcpRegistrationResult.location.uri)

      await(tcpRegistrationResult.unregister())

      await(locationService.list) shouldBe List.empty
      await(locationService.resolve(tcpConnection)) shouldBe None
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
      val httpRegistrationResult = await(locationService.register(httpRegistration))

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
      val tcpRegistrationResult = await(locationService.register(tcpRegistration))
      val httpRegistrationResult = await(locationService.register(httpRegistration))
      val akkaRegistrationResult = await(locationService.register(akkaRegistration))

      await(locationService.list).toSet shouldBe Set(tcpRegistrationResult.location, httpRegistrationResult.location, akkaRegistrationResult.location)
      await(locationService.list(ConnectionType.AkkaType)).toSet shouldBe Set(akkaRegistrationResult.location)
      await(locationService.list(ComponentType.Service)).toSet shouldBe Set(tcpRegistrationResult.location, httpRegistrationResult.location)
      await(locationService.list(new Networks().hostname())).toSet shouldBe Set(tcpRegistrationResult.location, httpRegistrationResult.location)
    }

    Await.result(assertionF, 5.seconds)
    //#filtering
  }

  test("shutdown") {
    //#shutdown
    Await.result(locationService.shutdown(), 20.seconds)
    //#shutdown
  }
}
