package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, LongKey, StringKey}
import csw.params.core.models.ObsId
import csw.params.events.OpticalDetectorEvent._
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.Table

class OpticalDetectorEventTest extends AnyFunSpec with Matchers {
  describe("OpticalDetectorEvent.scala") {

    it("create Observe Events with obsId | ESW-81") {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (ObserveStart.create("ESW.test", ObsId("some-id")), "ObserveStart", "ESW.test"),
        (ObserveEnd.create("ESW.test", ObsId("some-id")), "ObserveEnd", "ESW.test")
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix(expectedPrefixStr)
      })

    }

    it("create Observe Events with obsId and exposure id | ESW-81") {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (PrepareStart.create("ESW.test", ObsId("some-id"), "exp-id"), "PrepareStart", "ESW.test"),
        (ExposureStart.create("ESW.test", ObsId("some-id"), "exp-id"), "ExposureStart", "ESW.test"),
        (ExposureEnd.create("ESW.test", ObsId("some-id"), "exp-id"), "ExposureEnd", "ESW.test"),
        (ReadoutEnd.create("ESW.test", ObsId("some-id"), "exp-id"), "ReadoutEnd", "ESW.test"),
        (ReadoutFailed.create("ESW.test", ObsId("some-id"), "exp-id"), "ReadoutFailed", "ESW.test"),
        (DataWriteStart.create("ESW.test", ObsId("some-id"), "exp-id"), "DataWriteStart", "ESW.test"),
        (DataWriteEnd.create("ESW.test", ObsId("some-id"), "exp-id"), "DataWriteEnd", "ESW.test"),
        (ExposureAborted.create("ESW.test", ObsId("some-id"), "exp-id"), "ExposureAborted", "ESW.test")
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix(expectedPrefixStr)
      })
    }

    it("create OpticalDetectorExposureState event | ESW-81") {
      val detector     = "my-detector"
      val sourcePrefix = "ESW.test"
      val obsId        = ObsId("some-id")
      val observeEvent = OpticalDetectorExposureState.create(
        sourcePrefix,
        obsId,
        detector,
        exposureInProgress = true,
        abortInProgress = false,
        isAborted = false,
        "",
        OperationalState.BUSY
      )

      observeEvent.eventName shouldBe EventName("OpticalDetectorExposureState")
      observeEvent.source shouldBe Prefix(sourcePrefix)
      observeEvent.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
        StringKey.make("operationalState").set("BUSY"),
        StringKey.make("errorMessage").set(""),
        BooleanKey.make("exposureInProgress").set(true),
        BooleanKey.make("abortInProgress").set(false),
        BooleanKey.make("isAborted").set(false)
      )
    }

    it("create OpticalDetectorExposureData event | ESW-81") {
      val exposureTime          = 23923L
      val remainingExposureTime = 324335L
      val detector              = "my-detector"
      val sourcePrefix          = "ESW.test"
      val obsId                 = ObsId("some-id")
      val observeEvent = OpticalDetectorExposureData.create(
        sourcePrefix,
        obsId,
        detector,
        exposureTime,
        remainingExposureTime
      )

      observeEvent.eventName shouldBe EventName("OpticalDetectorExposureData")
      observeEvent.source shouldBe Prefix(sourcePrefix)
      observeEvent.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
    }

  }
}
