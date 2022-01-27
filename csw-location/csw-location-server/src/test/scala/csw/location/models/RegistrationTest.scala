package csw.location.models

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.location.client.ActorSystemFactory
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.network.utils.Networks
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RegistrationTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "my-actor-1")

  test("should able to create the AkkaRegistration which should internally create AkkaLocation without metadata | CSW-108") {
    val hostname = Networks().hostname

    val akkaConnection = AkkaConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
    val actorRef: ActorRef[_] = actorSystem.spawn(
      Behaviors.empty,
      "my-actor-3"
    )

    val akkaRegistration     = AkkaRegistrationFactory.make(akkaConnection, actorRef)
    val expectedAkkaLocation = AkkaLocation(akkaConnection, actorRef.toURI, Metadata.empty)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation
  }

  test("should able to create the AkkaRegistration with metadata which should internally create AkkaLocation | CSW-108") {
    val hostname = Networks().hostname

    val akkaConnection = AkkaConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
    val actorRef: ActorRef[_] = actorSystem.spawn(
      Behaviors.empty,
      "my-actor-4"
    )

    val metadata             = Metadata(Map("key1" -> "value"))
    val akkaRegistration     = AkkaRegistrationFactory.make(akkaConnection, actorRef, metadata)
    val expectedAkkaLocation = AkkaLocation(akkaConnection, actorRef.toURI, metadata)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation without metadata | CSW-108") {
    val hostname = Networks().hostname
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "trombone"), ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val expectedhttpLocation = HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"), Metadata.empty)

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test(
    "should able to create the HttpRegistration with metadata which should internally create HttpLocation with metadata | CSW-108"
  ) {
    val hostname = Networks().hostname
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "trombone"), ComponentType.HCD))
    val metadata         = Metadata(Map("key1" -> "value1"))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix, metadata)

    val expectedhttpLocation =
      HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"), metadata)

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation without metadata | CSW-108") {
    val hostname = Networks().hostname
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "lgsTrombone"), ComponentType.HCD))
    val tcpRegistration = TcpRegistration(tcpConnection, port)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"), Metadata.empty)

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should able to create the TcpRegistration with metadata which should internally create TcpLocation |  CSW-108") {
    val hostname = Networks().hostname
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "lgsTrombone"), ComponentType.HCD))
    val metadata        = Metadata(Map("key1" -> "value1"))
    val tcpRegistration = TcpRegistration(tcpConnection, port, metadata)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"), metadata)

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should not allow AkkaRegistration using local ActorRef") {
    val config: Config = ConfigFactory.parseString("""
        akka.actor.provider = local
      """)

    implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "local-actor-system", config)
    val actorRef                                                 = actorSystem.spawn(Behaviors.empty, "my-actor-2")
    val akkaConnection = AkkaConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))

    intercept[LocalAkkaActorRegistrationNotAllowed] {
      AkkaRegistrationFactory.make(akkaConnection, actorRef)
    }
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }
}
