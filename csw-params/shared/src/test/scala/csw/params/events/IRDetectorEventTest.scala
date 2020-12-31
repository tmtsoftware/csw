package csw.params.events

import csw.params.core.generics.KeyType.{BooleanKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.{KeyType, Parameter}
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
        (ExposureStart.create(sourcePrefix, obsId, exposureId), ExposureStart.entryName, sourcePrefix),
        (ExposureEnd.create(sourcePrefix, obsId, exposureId), ExposureEnd.entryName, sourcePrefix),
        (ReadoutEnd.create(sourcePrefix, obsId, exposureId), ReadoutEnd.entryName, sourcePrefix),
        (ReadoutFailed.create(sourcePrefix, obsId, exposureId), ReadoutFailed.entryName, sourcePrefix),
        (DataWriteStart.create(sourcePrefix, obsId, exposureId), DataWriteStart.entryName, sourcePrefix),
        (DataWriteEnd.create(sourcePrefix, obsId, exposureId), DataWriteEnd.entryName, sourcePrefix),
        (ExposureAborted.create(sourcePrefix, obsId, exposureId), ExposureAborted.entryName, sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create observe event with obsId | CSW-118") {
      Table(
        ("Observe event", "event name", "prefix"),
        (ObserveStart.create(sourcePrefix, obsId), ObserveStart.entryName, sourcePrefix),
        (ObserveEnd.create(sourcePrefix, obsId), ObserveEnd.entryName, sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe Prefix(sourcePrefix)
      })
    }

    it("should create exposure state event | CSW-118") {
      val state = IrDetectorExposureState.create(
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
      state.eventName.name shouldBe IrDetectorExposureState.entryName
      state.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
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

      val event = IrDetectorExposureData.create(
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
      event.eventName.name shouldBe IrDetectorExposureData.entryName
      event.paramSet shouldBe Set(
        StringKey.make("detector").set(detector),
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
