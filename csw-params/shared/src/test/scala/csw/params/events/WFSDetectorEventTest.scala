package csw.params.events

import csw.params.core.models.{ExposureId, ExposureIdType}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class WFSDetectorEventTest extends AnyFunSpec with Matchers {
  describe("WFS Detector") {
    val sourcePrefix               = Prefix("ESW.filter.wheel")
    val exposureId: ExposureIdType = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")

    it("should create publish success observe event | CSW-118, CSW-119") {
      val event = WFSDetectorEvent.publishSuccess(sourcePrefix)

      event.eventName.name shouldBe "ObserveEvent.PublishSuccess"
      event.source shouldBe sourcePrefix
    }

    it("should create publish fail observe event | CSW-118, CSW-119") {
      val event = WFSDetectorEvent.publishFail(sourcePrefix)

      event.eventName.name shouldBe "ObserveEvent.PublishFail"
      event.source shouldBe sourcePrefix
    }

    it("should create exposure state observe event | CSW-118, CSW-119") {
      val event = WFSDetectorEvent.exposureState(
        sourcePrefix,
        exposureId,
        exposureInProgress = true,
        abortInProgress = true,
        isAborted = true,
        OperationalState.BUSY,
        ""
      )

      event.eventName.name shouldBe "ObserveEvent.WfsDetectorExposureState"
      event.source shouldBe sourcePrefix
      event.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureId.toString),
        ObserveEventKeys.operationalState.set("BUSY"),
        ObserveEventKeys.errorMessage.set(""),
        ObserveEventKeys.exposureInProgress.set(true),
        ObserveEventKeys.abortInProgress.set(true),
        ObserveEventKeys.isAborted.set(true)
      )
    }
  }
}
