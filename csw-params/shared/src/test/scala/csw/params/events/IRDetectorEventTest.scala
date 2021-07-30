package csw.params.events

import csw.params.core.models.{ExposureId, ExposureIdType, ObsId}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class IRDetectorEventTest extends AnyFunSpec with Matchers {
  describe("IR detector") {
    val sourcePrefix                   = "ESW.filter.wheel"
    val obsId                          = ObsId("2020A-001-123")
    val exposureId                     = "12345"
    val detector                       = "ir-detector"
    val exposureIdType: ExposureIdType = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")
    it("should create observe event with obsId and exposure id parameters | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.exposureStart(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureStart", sourcePrefix),
        (IRDetectorEvent.exposureEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureEnd", sourcePrefix),
        (IRDetectorEvent.readoutEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.ReadoutEnd", sourcePrefix),
        (IRDetectorEvent.readoutFailed(sourcePrefix, obsId, exposureId), "ObserveEvent.ReadoutFailed", sourcePrefix),
        (IRDetectorEvent.dataWriteStart(sourcePrefix, obsId, exposureId), "ObserveEvent.DataWriteStart", sourcePrefix),
        (IRDetectorEvent.dataWriteEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.DataWriteEnd", sourcePrefix),
        (IRDetectorEvent.exposureAborted(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureAborted", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create observe event with obsId | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveEvent.ObserveStart", sourcePrefix),
        (IRDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEvent.ObserveEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create exposure state event | CSW-118, CSW-119") {
      val state = IRDetectorEvent.exposureState(
        sourcePrefix,
        obsId,
        detector,
        exposureInProgress = true,
        abortInProgress = false,
        isAborted = true,
        "",
        OperationalState.BUSY
      )

      state.source shouldBe Prefix(sourcePrefix)
      state.eventName.name shouldBe "ObserveEvent.IRDetectorExposureState"
      state.paramSet shouldBe Set(
        ObserveEventKeys.detector.set(detector),
        ObserveEventKeys.obsId.set(obsId.toString),
        ObserveEventKeys.exposureInProgress.set(true),
        ObserveEventKeys.abortInProgress.set(false),
        ObserveEventKeys.isAborted.set(true),
        ObserveEventKeys.errorMessage.set(""),
        ObserveEventKeys.operationalState.set("BUSY")
      )
    }

    it("should create exposure data event | CSW-118, CSW-119") {
      val readsInRamp           = 1
      val readsComplete         = 20
      val rampsInExposure       = 40
      val rampsComplete         = 50
      val exposureTime          = 1000L
      val remainingExposureTime = 20L

      val event = IRDetectorEvent.exposureData(
        sourcePrefix,
        exposureIdType,
        readsInRamp,
        readsComplete,
        rampsInExposure,
        rampsComplete,
        exposureTime,
        remainingExposureTime
      )

      event.source shouldBe Prefix(sourcePrefix)
      event.eventName.name shouldBe "ObserveEvent.IRDetectorExposureData"
      event.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureIdType.toString),
        ObserveEventKeys.readsInRamp.set(readsInRamp),
        ObserveEventKeys.readsComplete.set(readsComplete),
        ObserveEventKeys.rampsInExposure.set(rampsInExposure),
        ObserveEventKeys.rampsComplete.set(rampsComplete),
        ObserveEventKeys.exposureTime.set(exposureTime),
        ObserveEventKeys.remainingExposureTime.set(remainingExposureTime)
      )
    }
  }
}
