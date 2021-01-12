package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class IRDetectorEventTest extends AnyFunSpec with Matchers {
  describe("IR detector") {
    val sourcePrefix = "ESW.filter.wheel"
    val obsId        = ObsId("2020A-001-123")
    val exposureId   = "12345"
    val detector     = "ir-detector"

    it("should create observe event with obsId and exposure id parameters | CSW-118") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.exposureStart(sourcePrefix, obsId, exposureId), "ExposureStart", sourcePrefix),
        (IRDetectorEvent.exposureEnd(sourcePrefix, obsId, exposureId), "ExposureEnd", sourcePrefix),
        (IRDetectorEvent.readoutEnd(sourcePrefix, obsId, exposureId), "ReadoutEnd", sourcePrefix),
        (IRDetectorEvent.readoutFailed(sourcePrefix, obsId, exposureId), "ReadoutFailed", sourcePrefix),
        (IRDetectorEvent.dataWriteStart(sourcePrefix, obsId, exposureId), "DataWriteStart", sourcePrefix),
        (IRDetectorEvent.dataWriteEnd(sourcePrefix, obsId, exposureId), "DataWriteEnd", sourcePrefix),
        (IRDetectorEvent.exposureAborted(sourcePrefix, obsId, exposureId), "ExposureAborted", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create observe event with obsId | CSW-118") {
      Table(
        ("Observe event", "event name", "prefix"),
        (IRDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveStart", sourcePrefix),
        (IRDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create exposure state event | CSW-118") {
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
      state.eventName.name shouldBe "IRDetectorExposureState"
      state.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
        StringKey.make("obsId").set(obsId.toString),
        BooleanKey.make("exposureInProgress").set(true),
        BooleanKey.make("abortInProgress").set(false),
        BooleanKey.make("isAborted").set(true),
        StringKey.make("errorMessage").set(""),
        StringKey.make("operationalState").set("BUSY")
      )
    }

    it("should create exposure data event | CSW-118") {
      val readsInRamp           = 1
      val readsComplete         = 20
      val rampsInExposure       = 40
      val rampsComplete         = 50
      val exposureTime          = 1000L
      val remainingExposureTime = 20L

      val event = IRDetectorEvent.exposureData(
        sourcePrefix,
        obsId,
        detector,
        readsInRamp,
        readsComplete,
        rampsInExposure,
        rampsComplete,
        exposureTime,
        remainingExposureTime
      )

      event.source shouldBe Prefix(sourcePrefix)
      event.eventName.name shouldBe "IRDetectorExposureData"
      event.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
        StringKey.make("obsId").set(obsId.toString),
        IntKey.make("readsInRamp").set(readsInRamp),
        IntKey.make("readsComplete").set(readsComplete),
        IntKey.make("rampsInExposure").set(rampsInExposure),
        IntKey.make("rampsComplete").set(rampsComplete),
        LongKey.make("exposureTime").set(exposureTime),
        LongKey.make("remainingExposureTime").set(remainingExposureTime)
      )
    }
  }
}
