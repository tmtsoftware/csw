package csw.params.events

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.*

class SequencerObserveEventTest extends AnyFunSpec with Matchers {
  describe("SequencerObserveEvents") {
    val obsId                         = "2020A-001-123"
    val exposureId                    = ExposureId("2020A-001-123-TCS-DET-SCI0-0001")
    val prefix                        = Prefix(ESW, "filter.wheel")
    val obsIdParam: Parameter[_]      = ObserveEventKeys.obsId.set(obsId)
    val exposureIdParam: Parameter[_] = ObserveEventKeys.exposureId.set(exposureId.toString)
    val sequencerObserveEvent         = SequencerObserveEvent(prefix)
    val filename                      = "some/nested/folder/file123.conf"

    it("create Observe Event with obsId parameters | CSW-125") {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (sequencerObserveEvent.presetStart(ObsId(obsId)), "ObserveEvent.PresetStart", prefix),
        (sequencerObserveEvent.presetStart(ObsId(obsId)), "ObserveEvent.PresetStart", prefix),
        (sequencerObserveEvent.presetEnd(ObsId(obsId)), "ObserveEvent.PresetEnd", prefix),
        (sequencerObserveEvent.guidestarAcqStart(ObsId(obsId)), "ObserveEvent.GuidestarAcqStart", prefix),
        (sequencerObserveEvent.guidestarAcqEnd(ObsId(obsId)), "ObserveEvent.GuidestarAcqEnd", prefix),
        (sequencerObserveEvent.scitargetAcqStart(ObsId(obsId)), "ObserveEvent.ScitargetAcqStart", prefix),
        (sequencerObserveEvent.scitargetAcqEnd(ObsId(obsId)), "ObserveEvent.ScitargetAcqEnd", prefix),
        (sequencerObserveEvent.observationStart(ObsId(obsId)), "ObserveEvent.ObservationStart", prefix),
        (sequencerObserveEvent.observationEnd(ObsId(obsId)), "ObserveEvent.ObservationEnd", prefix),
        (sequencerObserveEvent.observeStart(ObsId(obsId)), "ObserveEvent.ObserveStart", prefix),
        (sequencerObserveEvent.observeEnd(ObsId(obsId)), "ObserveEvent.ObserveEnd", prefix)
      ).forEvery((observeEvent, expectedEventName, expectedPrefix) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(expectedPrefix)
        observeEvent.paramSet shouldBe Set(obsIdParam)
      })
    }

    it("create Observe Event with obsId and exposure Id parameters | CSW-125") {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (sequencerObserveEvent.exposureStart(exposureId), "ObserveEvent.ExposureStart", prefix),
        (sequencerObserveEvent.exposureEnd(exposureId), "ObserveEvent.ExposureEnd", prefix),
        (sequencerObserveEvent.readoutEnd(exposureId), "ObserveEvent.ReadoutEnd", prefix),
        (sequencerObserveEvent.readoutFailed(exposureId), "ObserveEvent.ReadoutFailed", prefix),
        (sequencerObserveEvent.prepareStart(exposureId), "ObserveEvent.PrepareStart", prefix),
        (sequencerObserveEvent.exposureAborted(exposureId), "ObserveEvent.ExposureAborted", prefix)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(expectedPrefixStr)
        observeEvent.paramSet shouldBe Set(exposureIdParam)
      })
    }

    it("should create observe event with exposureId and filename | CSW-118, CSW-119") {
      Table(
        ("Observe event", "event name", "prefix"),
        (sequencerObserveEvent.dataWriteStart(exposureId, filename), "ObserveEvent.DataWriteStart", prefix),
        (sequencerObserveEvent.dataWriteEnd(exposureId, filename), "ObserveEvent.DataWriteEnd", prefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
        observeEvent.paramSet shouldBe Set(exposureIdParam, ParamFactories.filenameParam(filename))
      })
    }

    it("create downtimeStart Observe Event with fixed Parameter set | CSW-125") {
      val event = sequencerObserveEvent.downtimeStart(ObsId(obsId), "bad weather")
      event.eventName should ===(EventName("ObserveEvent.DowntimeStart"))
      event.source should ===(prefix)
      event.paramSet shouldBe Set(obsIdParam, StringKey.make("reason").set("bad weather"))
    }

    it("create observePaused event | CSW-125") {
      val event = sequencerObserveEvent.observePaused()
      event.eventName should ===(EventName("ObserveEvent.ObservePaused"))
      event.source should ===(prefix)
    }

    it("create observeResumed event | CSW-125") {
      val event = sequencerObserveEvent.observeResumed()
      event.eventName should ===(EventName("ObserveEvent.ObserveResumed"))
      event.source should ===(prefix)
    }

    it("create offsetStart event | CSW-176") {
      val event = sequencerObserveEvent.offsetStart(ObsId(obsId), CoordinateSystem.XY, 10.0, 20.0)
      event.eventName should ===(EventName("ObserveEvent.OffsetStart"))
      event.source should ===(prefix)
      event.paramSet shouldBe Set(
        obsIdParam,
        ParamFactories.coordinateSystemParam(CoordinateSystem.XY),
        ParamFactories.pOffsetParam(10.0),
        ParamFactories.qOffsetParam(20.0)
      )
    }

    it("create offsetEnd event | CSW-176") {
      val event1 = sequencerObserveEvent.offsetEnd(ObsId(obsId))
      event1.eventName should ===(EventName("ObserveEvent.OffsetEnd"))
      event1.source should ===(prefix)
      event1.paramSet shouldBe Set(obsIdParam)
    }

    it("create inputRequestStart event | CSW-176") {
      val event = sequencerObserveEvent.inputRequestStart(ObsId(obsId))
      event.eventName should ===(EventName("ObserveEvent.InputRequestStart"))
      event.source should ===(prefix)
      event.paramSet shouldBe Set(obsIdParam)
    }

    it("create inputRequestEnd event | CSW-176") {
      val event = sequencerObserveEvent.inputRequestEnd(ObsId(obsId))
      event.eventName should ===(EventName("ObserveEvent.InputRequestEnd"))
      event.source should ===(prefix)
      event.paramSet shouldBe Set(obsIdParam)
    }
  }
}
