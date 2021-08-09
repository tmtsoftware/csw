package csw.params.events

import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class IRDetectorEventTest extends AnyFunSpec with Matchers {
  describe("IR detector") {
    val sourcePrefix           = Prefix("ESW.filter.wheel")
    val obsId                  = ObsId("2020A-001-123")
    val exposureId: ExposureId = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")
    val filename               = "some/nested/folder/file123.conf";

    it("should create observe event with obsId and exposure id parameters | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.exposureStart(sourcePrefix, exposureId), "ObserveEvent.ExposureStart", sourcePrefix),
        (IRDetectorEvent.exposureEnd(sourcePrefix, exposureId), "ObserveEvent.ExposureEnd", sourcePrefix),
        (IRDetectorEvent.readoutEnd(sourcePrefix, exposureId), "ObserveEvent.ReadoutEnd", sourcePrefix),
        (IRDetectorEvent.readoutFailed(sourcePrefix, exposureId), "ObserveEvent.ReadoutFailed", sourcePrefix),
        (IRDetectorEvent.exposureAborted(sourcePrefix, exposureId), "ObserveEvent.ExposureAborted", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
      })
    }

    it("should create observe event with obsId | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveEvent.ObserveStart", sourcePrefix),
        (IRDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEvent.ObserveEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set(ParamFactories.obsIdParam(obsId))
      })
    }

    it("should create observe event with exposureId and filename | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.dataWriteStart(sourcePrefix, exposureId, filename), "ObserveEvent.DataWriteStart", sourcePrefix),
        (IRDetectorEvent.dataWriteEnd(sourcePrefix, exposureId, filename), "ObserveEvent.DataWriteEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set(ParamFactories.exposureIdParam(exposureId), ParamFactories.filenameParam(filename))
      })
    }

    it("should create observe event without obsId | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.observeStart(sourcePrefix), "ObserveEvent.ObserveStart", sourcePrefix),
        (IRDetectorEvent.observeEnd(sourcePrefix), "ObserveEvent.ObserveEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set.empty
      })
    }

    it("should create exposure state event | CSW-118, CSW-119") {
      val state = IRDetectorEvent.exposureState(
        sourcePrefix,
        exposureId,
        exposureInProgress = true,
        abortInProgress = false,
        isAborted = true,
        "",
        OperationalState.BUSY
      )

      state.source shouldBe sourcePrefix
      state.eventName.name shouldBe "ObserveEvent.IRDetectorExposureState"
      state.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureId.toString),
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
        exposureId,
        readsInRamp,
        readsComplete,
        rampsInExposure,
        rampsComplete,
        exposureTime,
        remainingExposureTime
      )

      event.source shouldBe sourcePrefix
      event.eventName.name shouldBe "ObserveEvent.IRDetectorExposureData"
      event.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureId.toString),
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
