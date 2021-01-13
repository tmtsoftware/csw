package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, StringKey}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class WFSDetectorEventTest extends AnyFunSpec with Matchers {
  describe("WFS Detector") {
    it("should create publish success observe event") {
      val sourcePrefix = "ESW.filter.wheel"
      val event        = WFSDetectorEvent.publishSuccess(sourcePrefix)

      event.eventName.name shouldBe "PublishSuccess"
      event.source shouldBe Prefix(sourcePrefix)
    }

    it("should create publish fail observe event") {
      val sourcePrefix = "ESW.filter.wheel"
      val event        = WFSDetectorEvent.publishFail(sourcePrefix)

      event.eventName.name shouldBe "PublishFail"
      event.source shouldBe Prefix(sourcePrefix)
    }

    it("should create exposure state observe event") {
      val sourcePrefix = "ESW.filter.wheel"
      val detector     = "my-detector"
      val event = WFSDetectorEvent.exposureState(
        sourcePrefix,
        detector,
        exposureInProgress = true,
        abortInProgress = true,
        isAborted = true,
        OperationalState.BUSY,
        ""
      )

      event.eventName.name shouldBe "wfsDetectorExposureState"
      event.source shouldBe Prefix(sourcePrefix)
      event.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
        StringKey.make("operationalState").set("BUSY"),
        StringKey.make("errorMessage").set(""),
        BooleanKey.make("exposureInProgress").set(true),
        BooleanKey.make("abortInProgress").set(true),
        BooleanKey.make("isAborted").set(true)
      )
    }
  }
}
