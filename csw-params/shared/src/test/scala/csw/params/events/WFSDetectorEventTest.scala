package csw.params.events

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
  }
}
