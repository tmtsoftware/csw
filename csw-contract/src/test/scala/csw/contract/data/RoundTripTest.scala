package csw.contract.data

import csw.contract.generator.{ModelType, RoundTrip}
import io.bullet.borer.{Cbor, Json}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class RoundTripTest extends AnyFreeSpec with Matchers {

  CswData.services.data.foreach { case (serviceName, service) =>
    serviceName - {
      "models" - {
        service.models.modelTypes.foreach { modelType =>
          modelType.name - {
            validate(modelType)
          }
        }
      }

      "http requests" - {
        service.`http-contract`.requests.modelTypes.foreach { modelType =>
          validate(modelType)
        }
      }

      "websocket requests" - {
        service.`websocket-contract`.requests.modelTypes.foreach { modelType =>
          validate(modelType)
        }
      }
    }
  }

  private def validate(modelType: ModelType[_]): Unit = {
    modelType.models.zipWithIndex.foreach { case (modelData, index) =>
      s"${modelData.getClass.getSimpleName.stripSuffix("$")}: $index" - {
        List(Json, Cbor).foreach { format =>
          format.toString in {
            RoundTrip.roundTrip(modelData, modelType.codec, format) shouldBe modelData
          }
        }
      }
    }
  }
}
