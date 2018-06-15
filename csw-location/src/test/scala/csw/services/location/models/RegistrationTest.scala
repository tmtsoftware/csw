package csw.services.location.models

import java.net.URI

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, UntypedActorSystemOps}
import akka.actor.{ActorPath, ActorSystem}
import akka.serialization.Serialization
import com.typesafe.config.{Config, ConfigFactory}
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location._
import csw.services.location.commons.{ActorSystemFactory, LocationFactory, RegistrationFactory2}
import csw.services.location.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.services.location.internal.Networks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RegistrationTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val actorSystem: ActorSystem = ActorSystemFactory.remote()

  val RegistrationFactory = new RegistrationFactory2

  test("should able to create the AkkaRegistration which should internally create AkkaLocation") {
    val hostname = new Networks().hostname()

    val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef       = actorSystem.spawn(Behavior.empty, "my-actor-1")
    val actorPath      = ActorPath.fromString(Serialization.serializedActorPath(actorRef.toUntyped))
    val akkaUri        = new URI(actorPath.toString)

    val akkaRegistration = RegistrationFactory.akka(akkaConnection, actorRef)

    val expectedAkkaLocation = LocationFactory.akka(akkaConnection, akkaUri, actorRef)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation") {
    val hostname = new Networks().hostname()
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(ComponentId("trombone", ComponentType.HCD))
    val httpRegistration = RegistrationFactory.http(httpConnection, port, prefix)

    val expectedhttpLocation = LocationFactory.http(httpConnection, new URI(s"http://$hostname:$port/$prefix"))

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation") {
    val hostname = new Networks().hostname()
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId("lgsTrombone", ComponentType.HCD))
    val tcpRegistration = RegistrationFactory.tcp(tcpConnection, port)

    val expectedTcpLocation = LocationFactory.tcp(tcpConnection, new URI(s"tcp://$hostname:$port"))

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should not allow AkkaRegistration using local ActorRef") {
    val config: Config = ConfigFactory.parseString("""
        akka.actor.provider = local
      """)

    implicit val actorSystem: ActorSystem = ActorSystem("local-actor-system", config)
    val actorRef                          = actorSystem.spawn(Behavior.empty, "my-actor-2")
    val akkaConnection                    = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))

    intercept[LocalAkkaActorRegistrationNotAllowed] {
      RegistrationFactory.akka(akkaConnection, actorRef)
    }
    Await.result(actorSystem.terminate, 10.seconds)
  }

  override protected def afterAll(): Unit = {
    Await.result(actorSystem.terminate, 10.seconds)
    super.afterAll()
  }
}
