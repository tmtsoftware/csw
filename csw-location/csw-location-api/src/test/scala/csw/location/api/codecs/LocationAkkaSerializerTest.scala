package csw.location.api.codecs

import java.net.URI

import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Behavior}
import akka.serialization.SerializationExtension
import csw.location.api.models.ComponentType.{Assembly, Service}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpRegistration, LocationRemoved}
import csw.params.core.models.Prefix
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class LocationAkkaSerializerTest extends FunSuite with Matchers with BeforeAndAfterAll {

  // need to instantiate from remote factory to wire up serializer
  private final val system        = ActorSystem(Behavior.empty, "example")
  private final val serialization = SerializationExtension(system.toUntyped)
  private final val prefix        = Prefix("wfos.prog.cloudcover")

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  test("should use location serializer for location model (de)serialization") {
    val connection = AkkaConnection(ComponentId("TromboneAssembly", Assembly))
    val testData = Table(
      "Location models",
      connection,
      AkkaLocation(connection, prefix, new URI(""), system),
      HttpRegistration(HttpConnection(ComponentId("ConfigService", Service)), 1234, ""),
      LocationRemoved(connection)
    )

    forAll(testData) { locationModel ⇒
      val serializer = serialization.findSerializerFor(locationModel)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(locationModel)
      serializer.fromBinary(bytes, Some(locationModel.getClass)) shouldEqual locationModel
    }
  }
}
