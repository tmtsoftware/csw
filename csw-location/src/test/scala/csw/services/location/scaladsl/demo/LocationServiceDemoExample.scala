package csw.services.location.scaladsl.demo

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest._

import async.Async._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

class LocationServiceDemoExample extends FunSuite with Matchers {

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

  private lazy val locationService = LocationServiceFactory.make()

  val tcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
  val tcpRegistration = TcpRegistration(tcpConnection, 6380)

  val httpConnection = HttpConnection(ComponentId("configuration", ComponentType.Service))
  val httpRegistration = HttpRegistration(httpConnection, 6380, "path123")

  val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
  val akkaRegistration = AkkaRegistration(akkaConnection, actorRef)

  test("demo") {
    val assertionF: Future[Assertion] = async {
      val registrationResult = await(locationService.register(tcpRegistration))

      registrationResult.location.connection shouldBe tcpConnection

      await(locationService.list) shouldBe List(registrationResult.location)
      await(locationService.resolve(tcpConnection)) shouldBe Some(registrationResult.location)

      println(registrationResult.location.uri)

      await(registrationResult.unregister())

      await(locationService.list) shouldBe List.empty
      await(locationService.resolve(tcpConnection)) shouldBe None
    }

    Await.result(assertionF, 5.seconds)
  }

  test("tracking") {
    val (switch, doneF) = locationService.track(tcpConnection).toMat(Sink.foreach(println))(Keep.both).run()

    Thread.sleep(200)

    async {
      val registrationResult = await(locationService.register(tcpRegistration))
      val registrationResult2 = await(locationService.register(httpRegistration))

      Thread.sleep(200)

      await(registrationResult.unregister())
      await(locationService.unregister(httpConnection))

      Thread.sleep(200)
      switch.shutdown()
    }

    Await.result(doneF, 5.seconds)
  }

  test("filtering") {

    val assertionF: Future[Assertion] = async {
      val registrationResult = await(locationService.register(tcpRegistration))
      val registrationResult2 = await(locationService.register(httpRegistration))
      val registrationResult3 = await(locationService.register(akkaRegistration))

      await(locationService.list).toSet shouldBe Set(registrationResult.location, registrationResult2.location, registrationResult3.location)

      await(locationService.list(ConnectionType.AkkaType)).toSet shouldBe Set(registrationResult3.location)
      await(locationService.list(ComponentType.Service)).toSet shouldBe Set(registrationResult.location, registrationResult2.location)
      await(locationService.list(new Networks().hostname())).toSet shouldBe Set(registrationResult.location, registrationResult2.location)
    }

    Await.result(assertionF, 5.seconds)

  }

  test("shutdown") {
    Await.result(locationService.shutdown(), 5.seconds)
  }
}
