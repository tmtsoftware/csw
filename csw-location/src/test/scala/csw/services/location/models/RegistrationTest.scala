package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorSystem}
import akka.serialization.Serialization
import akka.testkit.{ImplicitSender, TestKit}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import org.scalatest.{FunSuiteLike, Matchers}

class RegistrationTest extends TestKit(ActorSystem("testkit")) with ImplicitSender with FunSuiteLike with Matchers {

  test("should able to create the AkkaRegistration which should internally create AkkaLocation") {
    val hostname = new Networks().hostname()
    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(testActor))
    val akkaUri = new URI(actorPath.toString)

    val akkaConnection = AkkaConnection(new ComponentId("assembly", ComponentType.Container))
    val akkaRegistration = AkkaRegistration(akkaConnection, testActor)

    val expectedAkkaLocation = AkkaLocation(akkaConnection, akkaUri, testActor)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation") {
    val hostname = new Networks().hostname()
    val port = 9595
    val prefix = "/trombone/hcd"

    val httpConnection = HttpConnection(ComponentId("trombone", ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val expectedhttpLocation = HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"))

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation") {
    val hostname = new Networks().hostname()
    val port = 9596

    val tcpConnection = TcpConnection(ComponentId("lgsTrombone", ComponentType.HCD))
    val tcpRegistration = TcpRegistration(tcpConnection, port)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"))

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

}
