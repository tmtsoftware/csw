package csw.contract.data

import csw.contract.generator.RoundTrip
import io.bullet.borer.{Cbor, Json}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class RoundTripTest extends AnyFreeSpec with Matchers {

  CswData.services.data.foreach {
    case (serviceName, service) =>
      serviceName - {
        service.models.modelTypes.foreach { modelType =>
          modelType.name - {
            List(Json, Cbor).foreach { format =>
              format.toString - {
                modelType.models.zipWithIndex.foreach {
                  case (modelData, index) =>
                    s"data index: $index" in {
                      RoundTrip.roundTrip(modelData, modelType.codec, format) shouldBe modelData
                    }
                }
              }
            }
          }
        }
      }
  }
}
