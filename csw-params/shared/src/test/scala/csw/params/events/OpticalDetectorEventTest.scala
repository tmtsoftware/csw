package csw.params.events

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.{ExposureId, ExposureIdType, ObsId}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.Table

class OpticalDetectorEventTest extends AnyFunSpec with Matchers {
  describe("OpticalDetectorEvent") {

    it("create Observe Events with obsId | ESW-118, CSW-119") {
      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.observeStart("ESW.test", ObsId("2020A-001-123")), "ObserveEvent.ObserveStart"),
        (OpticalDetectorEvent.observeEnd("ESW.test", ObsId("2020A-001-123")), "ObserveEvent.ObserveEnd")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix("ESW.test")
        observeEvent.paramSet shouldBe Set(StringKey.make("obsId").set("2020A-001-123"))
      })
    }

    it("create Observe Events with obsId and exposure id | ESW-118, CSW-119") {
      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.prepareStart("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.PrepareStart"),
        (OpticalDetectorEvent.exposureStart("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.ExposureStart"),
        (OpticalDetectorEvent.exposureEnd("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.ExposureEnd"),
        (OpticalDetectorEvent.readoutEnd("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.ReadoutEnd"),
        (OpticalDetectorEvent.readoutFailed("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.ReadoutFailed"),
        (OpticalDetectorEvent.dataWriteStart("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.DataWriteStart"),
        (OpticalDetectorEvent.dataWriteEnd("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.DataWriteEnd"),
        (OpticalDetectorEvent.exposureAborted("ESW.test", ObsId("2020A-001-123"), "exp-id"), "ObserveEvent.ExposureAborted")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix("ESW.test")
        observeEvent.paramSet shouldBe Set(
          StringKey.make("obsId").set("2020A-001-123"),
          StringKey.make("exposureId").set("exp-id")
        )
      })
    }

    it("create OpticalDetectorExposureState event | ESW-118, CSW-119") {
      val detector     = "my-detector"
      val sourcePrefix = "ESW.test"
      val obsId        = ObsId("2020A-001-123")
      val observeEvent = OpticalDetectorEvent.exposureState(
        sourcePrefix,
        obsId,
        detector,
        exposureInProgress = true,
        abortInProgress = false,
        isAborted = false,
        "",
        OperationalState.BUSY
      )

      observeEvent.eventName shouldBe EventName("ObserveEvent.OpticalDetectorExposureState")
      observeEvent.source shouldBe Prefix(sourcePrefix)
      observeEvent.paramSet shouldBe Set(
        ObserveEventKeys.detector.set(detector),
        ObserveEventKeys.obsId.set("2020A-001-123"),
        ObserveEventKeys.operationalState.set("BUSY"),
        ObserveEventKeys.errorMessage.set(""),
        ObserveEventKeys.exposureInProgress.set(true),
        ObserveEventKeys.abortInProgress.set(false),
        ObserveEventKeys.isAborted.set(false)
      )
    }

    it("create OpticalDetectorExposureData event | ESW-118, CSW-119") {
      val exposureTime                   = 23923L
      val remainingExposureTime          = 324335L
      val sourcePrefix                   = "ESW.test"
      val exposureIdType: ExposureIdType = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")
      val observeEvent = OpticalDetectorEvent.exposureData(
        sourcePrefix,
        exposureIdType,
        exposureTime,
        remainingExposureTime
      )

      observeEvent.eventName shouldBe EventName("ObserveEvent.OpticalDetectorExposureData")
      observeEvent.source shouldBe Prefix(sourcePrefix)
      observeEvent.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureIdType.toString),
        ObserveEventKeys.exposureTime.set(exposureTime),
        ObserveEventKeys.remainingExposureTime.set(remainingExposureTime)
      )
    }

  }
}
