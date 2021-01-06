package csw.params.events

import csw.params.events.WFSDetectorEvent.{PublishFail, PublishSuccess}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class WFSDetectorEventTest extends AnyFunSpec with Matchers {
  describe("WFS Detector") {
    it("should create publish success observe event") {
      val sourcePrefix = "ESW.filter.wheel"
      val event        = PublishSuccess.create(sourcePrefix)

      event.eventName.name shouldBe PublishSuccess.entryName
      event.source shouldBe Prefix(sourcePrefix)
    }

    it("should create publish fail observe event") {
      val sourcePrefix = "ESW.filter.wheel"
      val event        = PublishFail.create(sourcePrefix)

      event.eventName.name shouldBe PublishFail.entryName
      event.source shouldBe Prefix(sourcePrefix)
    }
  }
}
