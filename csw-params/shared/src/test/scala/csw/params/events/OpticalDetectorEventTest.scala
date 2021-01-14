package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, LongKey, StringKey}
import csw.params.core.models.ObsId
import csw.params.events.IRDetectorEvent.observeEventPrefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.Table

class OpticalDetectorEventTest extends AnyFunSpec with Matchers {
  describe("OpticalDetectorEvent") {

    val obsId        = ObsId("2020A-001-123")
    val sourcePrefix = "ESW.test"

    it("create Observe Events with obsId | ESW-118, CSW-119") {
      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveStart"),
        (OpticalDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEnd")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe observeEventPrefix
        observeEvent.paramSet shouldBe Set(
          StringKey.make("sourcePrefix").set(sourcePrefix),
          StringKey.make("obsId").set(obsId.toString)
        )
      })
    }

    it("create Observe Events with obsId and exposure id | ESW-118, CSW-119") {
      val exposureId = "exp-id"

      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.prepareStart(sourcePrefix, obsId, exposureId), "PrepareStart"),
        (OpticalDetectorEvent.exposureStart(sourcePrefix, obsId, exposureId), "ExposureStart"),
        (OpticalDetectorEvent.exposureEnd(sourcePrefix, obsId, exposureId), "ExposureEnd"),
        (OpticalDetectorEvent.readoutEnd(sourcePrefix, obsId, exposureId), "ReadoutEnd"),
        (OpticalDetectorEvent.readoutFailed(sourcePrefix, obsId, exposureId), "ReadoutFailed"),
        (OpticalDetectorEvent.dataWriteStart(sourcePrefix, obsId, exposureId), "DataWriteStart"),
        (OpticalDetectorEvent.dataWriteEnd(sourcePrefix, obsId, exposureId), "DataWriteEnd"),
        (OpticalDetectorEvent.exposureAborted(sourcePrefix, obsId, exposureId), "ExposureAborted")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe observeEventPrefix
        observeEvent.paramSet shouldBe Set(
          StringKey.make("sourcePrefix").set(sourcePrefix),
          StringKey.make("obsId").set(obsId.toString),
          StringKey.make("exposureId").set(exposureId)
        )
      })
    }

    it("create OpticalDetectorExposureState event | ESW-118, CSW-119") {
      val detector = "my-detector"
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

      observeEvent.eventName shouldBe EventName("OpticalDetectorExposureState")
      observeEvent.source shouldBe observeEventPrefix
      observeEvent.paramSet shouldBe Set(
        StringKey.make("sourcePrefix").set(sourcePrefix),
        StringKey.make("detector").set(detector),
        StringKey.make("obsId").set("2020A-001-123"),
        StringKey.make("operationalState").set("BUSY"),
        StringKey.make("errorMessage").set(""),
        BooleanKey.make("exposureInProgress").set(true),
        BooleanKey.make("abortInProgress").set(false),
        BooleanKey.make("isAborted").set(false)
      )
    }

    it("create OpticalDetectorExposureData event | ESW-118, CSW-119") {
      val exposureTime          = 23923L
      val remainingExposureTime = 324335L
      val detector              = "my-detector"
      val observeEvent = OpticalDetectorEvent.exposureData(
        sourcePrefix,
        obsId,
        detector,
        exposureTime,
        remainingExposureTime
      )

      observeEvent.eventName shouldBe EventName("OpticalDetectorExposureData")
      observeEvent.source shouldBe observeEventPrefix
      observeEvent.paramSet shouldBe Set(
        StringKey.make("sourcePrefix").set(sourcePrefix),
        StringKey.make("detector").set(detector),
        StringKey.make("obsId").set(obsId.toString),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
    }

  }
}
