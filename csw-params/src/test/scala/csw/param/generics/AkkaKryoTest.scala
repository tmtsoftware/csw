package csw.param.generics

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.twitter.chill.akka.AkkaSerializer
import csw.param.models.RaDec
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class AkkaKryoTest extends FunSuite with Matchers {
  test("demo") {
    val system = ActorSystem("example")

    val serialization = SerializationExtension(system)

    val original = RaDec(100, 33)

    val serializer = serialization.findSerializerFor(original)

    serializer.getClass shouldBe classOf[AkkaSerializer]

    serializer.fromBinary(serializer.toBinary(original)) shouldBe original

    Await.result(system.terminate(), 2.seconds)
  }
}
