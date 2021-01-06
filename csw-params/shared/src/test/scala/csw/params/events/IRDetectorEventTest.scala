package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.models.ObsId
import csw.params.events.IRDetectorEvent._
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class IRDetectorEventTest extends AnyFunSpec with Matchers {
  describe("IR detector") {
    val sourcePrefix = "ESW.filter.wheel"
    val obsId        = ObsId("someObsId")
    val exposureId   = "12345"
    val detector     = "ir-detector"

    it("should create observe event with obsId and exposure id parameters | CSW-118") {
      Table(
        ("Observe event", "event name", "prefix"),
        (ExposureStart.create(sourcePrefix, obsId, exposureId), "ExposureStart", sourcePrefix),
        (ExposureEnd.create(sourcePrefix, obsId, exposureId), "ExposureEnd", sourcePrefix),
        (ReadoutEnd.create(sourcePrefix, obsId, exposureId), "ReadoutEnd", sourcePrefix),
        (ReadoutFailed.create(sourcePrefix, obsId, exposureId), "ReadoutFailed", sourcePrefix),
        (DataWriteStart.create(sourcePrefix, obsId, exposureId), "DataWriteStart", sourcePrefix),
        (DataWriteEnd.create(sourcePrefix, obsId, exposureId), "DataWriteEnd", sourcePrefix),
        (ExposureAborted.create(sourcePrefix, obsId, exposureId), "ExposureAborted", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create observe event with obsId | CSW-118") {
      Table(
        ("Observe event", "event name", "prefix"),
        (ObserveStart.create(sourcePrefix, obsId), "ObserveStart", sourcePrefix),
        (ObserveEnd.create(sourcePrefix, obsId), "ObserveEnd", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create exposure state event | CSW-118") {
      val state = IRDetectorExposureState.create(
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
        StringKey.make("obsId").set(obsId.obsId),
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

      val event = IRDetectorExposureData.create(
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
        StringKey.make("obsId").set(obsId.obsId),
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
