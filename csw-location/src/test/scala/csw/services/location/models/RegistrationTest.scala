package csw.services.location.models

import java.net.URI

import akka.actor.{Actor, ActorPath, ActorSystem, Props}
import akka.serialization.Serialization
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.scaladsl.ActorSystemFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RegistrationTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("should able to create the AkkaRegistration which should internally create AkkaLocation") {
    val hostname = new Networks().hostname()

    val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorSystem    = ActorSystemFactory.remote
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-1"
    )

    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))
    val akkaUri   = new URI(actorPath.toString)

    val akkaRegistration = AkkaRegistration(akkaConnection, actorRef)

    val expectedAkkaLocation = AkkaLocation(akkaConnection, akkaUri, actorRef)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation

    Await.result(actorSystem.terminate, 10.seconds)
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation") {
    val hostname = new Networks().hostname()
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(ComponentId("trombone", ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val expectedhttpLocation = HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"))

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation") {
    val hostname = new Networks().hostname()
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId("lgsTrombone", ComponentType.HCD))
    val tcpRegistration = TcpRegistration(tcpConnection, port)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"))

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should not allow AkkaRegistration using local ActorRef") {
    val config: Config = ConfigFactory.parseString("""
        akka.actor.provider = local
      """)

    val actorSystem = ActorSystem("local-actor-system", config)
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-2"
    )
    val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))

    intercept[LocalAkkaActorRegistrationNotAllowed] {
      AkkaRegistration(akkaConnection, actorRef)
    }
    Await.result(actorSystem.terminate, 10.seconds)
  }
}
