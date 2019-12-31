package csw.location.models

import java.net.URI

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.client.ActorSystemFactory
import csw.location.models
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.network.utils.Networks
import csw.prefix.models.Subsystem
import csw.prefix.models.Prefix
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RegistrationTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val actorSystem: typed.ActorSystem[_] = ActorSystemFactory.remote(Behaviors.empty, "my-actor-1")

  test("should able to create the AkkaRegistration which should internally create AkkaLocation") {
    val hostname = Networks().hostname

    val akkaConnection = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
    val actorRefUri    = actorSystem.toURI

    val akkaRegistration     = AkkaRegistrationFactory.make(akkaConnection, actorRefUri)
    val expectedAkkaLocation = AkkaLocation(akkaConnection, actorRefUri)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation") {
    val hostname = Networks().hostname
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "trombone"), ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val expectedhttpLocation = HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"))

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation") {
    val hostname = Networks().hostname
    val port     = 9596

    val tcpConnection   = TcpConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "lgsTrombone"), ComponentType.HCD))
    val tcpRegistration = TcpRegistration(tcpConnection, port)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"))

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should not allow AkkaRegistration using local ActorRef") {
    val config: Config = ConfigFactory.parseString("""
        akka.actor.provider = local
      """)

    implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "local-actor-system", config)
    val actorRefURI                                              = actorSystem.spawn(Behaviors.empty, "my-actor-2").toURI
    val akkaConnection                                           = AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))

    intercept[LocalAkkaActorRegistrationNotAllowed] {
      AkkaRegistrationFactory.make(akkaConnection, actorRefURI)
    }
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }
}
