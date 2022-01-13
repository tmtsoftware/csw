package csw.params.events

import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.Table

class OpticalDetectorEventTest extends AnyFunSpec with Matchers {
  describe("OpticalDetectorEvent") {
    val sourcePrefix: Prefix   = Prefix("ESW.test")
    val exposureId: ExposureId = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")
    val obsId: ObsId           = ObsId("2020A-001-123")
    val filename               = "some/nested/folder/file123.conf"

    it("create Observe Events with obsId | ESW-118, CSW-119") {
      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveEvent.ObserveStart"),
        (OpticalDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEvent.ObserveEnd")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set(ParamFactories.obsIdParam(obsId))
      })
    }

    it("create Observe Events without obsId | ESW-118, CSW-119") {
      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.observeStart(sourcePrefix), "ObserveEvent.ObserveStart"),
        (OpticalDetectorEvent.observeEnd(sourcePrefix), "ObserveEvent.ObserveEnd")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set.empty
      })
    }

    it("should create observe event with exposureId and filename | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (OpticalDetectorEvent.dataWriteStart(sourcePrefix, exposureId, filename), "ObserveEvent.DataWriteStart", sourcePrefix),
        (OpticalDetectorEvent.dataWriteEnd(sourcePrefix, exposureId, filename), "ObserveEvent.DataWriteEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set(ParamFactories.exposureIdParam(exposureId), ParamFactories.filenameParam(filename))
      })
    }

    it("create Observe Events with obsId and exposure id | ESW-118, CSW-119") {
      Table(
        ("Observe Event", "Event Name"),
        (OpticalDetectorEvent.prepareStart(sourcePrefix, exposureId), "ObserveEvent.PrepareStart"),
        (OpticalDetectorEvent.exposureStart(sourcePrefix, exposureId), "ObserveEvent.ExposureStart"),
        (OpticalDetectorEvent.exposureEnd(sourcePrefix, exposureId), "ObserveEvent.ExposureEnd"),
        (OpticalDetectorEvent.readoutEnd(sourcePrefix, exposureId), "ObserveEvent.ReadoutEnd"),
        (OpticalDetectorEvent.readoutFailed(sourcePrefix, exposureId), "ObserveEvent.ReadoutFailed"),
        (OpticalDetectorEvent.exposureAborted(sourcePrefix, exposureId), "ObserveEvent.ExposureAborted")
      ).forEvery((observeEvent, expectedEventName) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set(
          ObserveEventKeys.exposureId.set(exposureId.toString)
        )
      })
    }

    it("create OpticalDetectorExposureState event | ESW-118, CSW-119") {
      val observeEvent = OpticalDetectorEvent.exposureState(
        sourcePrefix,
        exposureId,
        exposureInProgress = true,
        abortInProgress = false,
        isAborted = false,
        "",
        OperationalState.BUSY
      )

      observeEvent.eventName shouldBe EventName("ObserveEvent.OpticalDetectorExposureState")
      observeEvent.source shouldBe sourcePrefix
      observeEvent.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureId.toString),
        ObserveEventKeys.operationalState.set("BUSY"),
        ObserveEventKeys.errorMessage.set(""),
        ObserveEventKeys.exposureInProgress.set(true),
        ObserveEventKeys.abortInProgress.set(false),
        ObserveEventKeys.isAborted.set(false)
      )
    }

    it("create OpticalDetectorExposureData event | ESW-118, CSW-119") {
      val exposureTime           = 23923L
      val remainingExposureTime  = 324335L
      val exposureId: ExposureId = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")
      val coaddsDone             = 2
      val coaddsInExposure       = 4
      val observeEvent = OpticalDetectorEvent.exposureData(
        sourcePrefix,
        exposureId,
        coaddsInExposure,
        coaddsDone,
        exposureTime,
        remainingExposureTime
      )

      observeEvent.eventName shouldBe EventName("ObserveEvent.OpticalDetectorExposureData")
      observeEvent.source shouldBe sourcePrefix
      observeEvent.paramSet shouldBe Set(
        ObserveEventKeys.exposureId.set(exposureId.toString),
        ObserveEventKeys.exposureTime.set(exposureTime),
        ObserveEventKeys.remainingExposureTime.set(remainingExposureTime),
        ObserveEventKeys.coaddsInExposure.set(coaddsInExposure),
        ObserveEventKeys.coaddsDone.set(coaddsDone)
      )
    }

  }
}
