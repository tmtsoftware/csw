package csw.logging.client.cbor

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.actor.typed
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.serialization.SerializationExtension
import csw.logging.models.{Level, LogMetadata}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LoggingAkkaSerializerTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private final implicit val system: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "example")
  private final val serialization                                       = SerializationExtension(system.toClassic)

  test("should use Logging serializer for LogMetaData (de)serialization") {

    val logMetadata = LogMetadata(Level.INFO, Level.INFO, Level.INFO, Level.INFO)
    val serializer  = serialization.findSerializerFor(logMetadata)
    serializer.getClass shouldBe classOf[LoggingAkkaSerializer]

    val bytes = serializer.toBinary(logMetadata)
    serializer.fromBinary(bytes, Some(logMetadata.getClass)) shouldEqual logMetadata
  }

  test("should use Logging serializer for log Level (de)serialization") {
    val testData = Table(
      "Level models",
      Level.TRACE,
      Level.DEBUG,
      Level.INFO,
      Level.WARN,
      Level.ERROR,
      Level.FATAL
    )

    forAll(testData) { level =>
      val serializer = serialization.findSerializerFor(level)
      serializer.getClass shouldBe classOf[LoggingAkkaSerializer]

      val bytes = serializer.toBinary(level)
      serializer.fromBinary(bytes, Some(level.getClass)) shouldEqual level
    }
  }

}
