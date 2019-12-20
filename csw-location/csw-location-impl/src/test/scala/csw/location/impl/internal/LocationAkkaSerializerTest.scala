package csw.location.impl.internal

import java.net.URI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.serialization.SerializationExtension
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models
import csw.location.models.ComponentType.Assembly
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models._
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

private[location] class LocationAkkaSerializerTest extends FunSuite with Matchers with BeforeAndAfterAll {
  // need to instantiate from remote factory to wire up serializer
  private final implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "example")
  private final val serialization                   = SerializationExtension(system.toClassic)
  private final val prefix                          = Prefix(Subsystem.NFIRAOS, "TromboneAssembly")

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  test("should use location serializer for Connection (de)serialization") {
    val testData = Table(
      "Connection models",
      AkkaConnection(ComponentId(prefix, Assembly)),
      HttpConnection(ComponentId(prefix, Assembly)),
      TcpConnection(models.ComponentId(prefix, Assembly))
    )

    forAll(testData) { connection =>
      val serializer = serialization.findSerializerFor(connection)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(connection)
      serializer.fromBinary(bytes, Some(connection.getClass)) shouldEqual connection
    }
  }

  test("should use location serializer for Location (de)serialization") {
    val akkaConnection = AkkaConnection(models.ComponentId(prefix, Assembly))
    val httpConnection = HttpConnection(models.ComponentId(prefix, Assembly))
    val tcpConnection  = TcpConnection(models.ComponentId(prefix, Assembly))
    val testData = Table(
      "Location models",
      AkkaLocation(akkaConnection, system.toURI),
      HttpLocation(httpConnection, new URI("")),
      TcpLocation(tcpConnection, new URI(""))
    )

    forAll(testData) { location =>
      val serializer = serialization.findSerializerFor(location)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(location)
      serializer.fromBinary(bytes, Some(location.getClass)) shouldEqual location
    }
  }

  test("should use location serializer for Registration (de)serialization") {
    val akkaConnection = AkkaConnection(models.ComponentId(prefix, Assembly))
    val httpConnection = HttpConnection(models.ComponentId(prefix, Assembly))
    val tcpConnection  = TcpConnection(models.ComponentId(prefix, Assembly))
    val testData = Table(
      "Registration models",
      AkkaRegistration(akkaConnection, system.toURI),
      HttpRegistration(httpConnection, 1234, ""),
      TcpRegistration(tcpConnection, 1234)
    )

    forAll(testData) { registration =>
      val serializer = serialization.findSerializerFor(registration)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(registration)
      serializer.fromBinary(bytes, Some(registration.getClass)) shouldEqual registration
    }
  }

  test("should use location serializer for TrackingEvent (de)serialization") {
    val akkaConnection = AkkaConnection(models.ComponentId(prefix, Assembly))
    val akkaLocation   = AkkaLocation(akkaConnection, system.toURI)

    val testData = Table(
      "TrackingEvent models",
      LocationUpdated(akkaLocation),
      LocationRemoved(akkaConnection)
    )

    forAll(testData) { trackingEvent =>
      val serializer = serialization.findSerializerFor(trackingEvent)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(trackingEvent)
      serializer.fromBinary(bytes, Some(trackingEvent.getClass)) shouldEqual trackingEvent
    }
  }
}
