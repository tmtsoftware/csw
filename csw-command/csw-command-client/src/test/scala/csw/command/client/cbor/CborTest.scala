package csw.command.client.cbor

import akka.actor.typed.ActorSystem
import csw.command.client.models.framework.LocationServiceUsage
import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterAndTrackServices, RegisterOnly}
import io.bullet.borer.Cbor
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.{FunSuite, Matchers}

object MessageCodec extends MessageCodecs {
  override implicit def actorSystem: ActorSystem[_] = ???
}

class CborTest extends FunSuite with Matchers {
  import MessageCodec.locationServiceUsageCodec
  test("should encode concrete-type LocationServiceUsage and decode base-type") {
    val testData = Table(
      "LocationServiceUsage models",
      DoNotRegister,
      RegisterOnly,
      RegisterAndTrackServices
    )

    forAll(testData) { locationServiceUsage ⇒
      val bytes = Cbor.encode[LocationServiceUsage](locationServiceUsage).toByteArray
      Cbor.decode(bytes).to[LocationServiceUsage].value shouldEqual locationServiceUsage
    }
  }
}
