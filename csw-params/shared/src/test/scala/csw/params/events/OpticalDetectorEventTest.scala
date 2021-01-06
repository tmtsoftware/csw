package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, LongKey, StringKey}
import csw.params.core.models.ObsId
import csw.params.events.OpticalDetectorEvent._
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.Table

class OpticalDetectorEventTest extends AnyFunSpec with Matchers {
  describe("OpticalDetectorEvent") {

    it("create Observe Events with obsId | ESW-81") {
      Table(
        ("Observe Event", "Event Name"),
        (ObserveStart.create("ESW.test", ObsId("some-id")), "ObserveStart"),
        (ObserveEnd.create("ESW.test", ObsId("some-id")), "ObserveEnd")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix("ESW.test")
        observeEvent.paramSet shouldBe Set(StringKey.make("obsId").set("some-id"))
      })

    }

    it("create Observe Events with obsId and exposure id | ESW-81") {
      Table(
        ("Observe Event", "Event Name"),
        (PrepareStart.create("ESW.test", ObsId("some-id"), "exp-id"), "PrepareStart"),
        (ExposureStart.create("ESW.test", ObsId("some-id"), "exp-id"), "ExposureStart"),
        (ExposureEnd.create("ESW.test", ObsId("some-id"), "exp-id"), "ExposureEnd"),
        (ReadoutEnd.create("ESW.test", ObsId("some-id"), "exp-id"), "ReadoutEnd"),
        (ReadoutFailed.create("ESW.test", ObsId("some-id"), "exp-id"), "ReadoutFailed"),
        (DataWriteStart.create("ESW.test", ObsId("some-id"), "exp-id"), "DataWriteStart"),
        (DataWriteEnd.create("ESW.test", ObsId("some-id"), "exp-id"), "DataWriteEnd"),
        (ExposureAborted.create("ESW.test", ObsId("some-id"), "exp-id"), "ExposureAborted")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix("ESW.test")
        observeEvent.paramSet shouldBe Set(
          StringKey.make("obsId").set("some-id"),
          StringKey.make("exposureId").set("exp-id")
        )
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
        StringKey.make("obsId").set("some-id"),
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
        StringKey.make("obsId").set(obsId.obsId),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
    }

  }
}
