package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.ExposureId;
import csw.params.core.models.ObsId;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

public class JSequencerObserveEventTest extends JUnitSuite {
    final Prefix prefix = new Prefix(JSubsystem.ESW, "filter.wheel");
    final ObsId obsId = ObsId.apply("2020A-001-123");
    final String exposureId = "2021A-001-123-TCS-DET-SCI2-1234";
    final Parameter<String> exposureIdParam = ObserveEventKeys.exposureId().set(exposureId);
    final Parameter<String> obsIdParam = ObserveEventKeys.obsId().set(obsId.toString());
    final Parameter<String> downTimeParam = ObserveEventKeys.downTimeReason().set("infra failure");
    final SequencerObserveEvent sequencerObserveEvent = new SequencerObserveEvent(prefix);

    @Test
    public void createObserveEventwithObsIdParameters__CSW_125() {
        List<TestData> testData = new ArrayList<>(Arrays.asList(
                new TestData(sequencerObserveEvent.presetStart(obsId), "ObserveEvent.PresetStart", prefix),
                new TestData(sequencerObserveEvent.presetStart(obsId), "ObserveEvent.PresetStart", prefix),
                new TestData(sequencerObserveEvent.presetEnd(obsId), "ObserveEvent.PresetEnd", prefix),
                new TestData(sequencerObserveEvent.guidestarAcqStart(obsId), "ObserveEvent.GuidestarAcqStart", prefix),
                new TestData(sequencerObserveEvent.guidestarAcqEnd(obsId), "ObserveEvent.GuidestarAcqEnd", prefix),
                new TestData(sequencerObserveEvent.scitargetAcqStart(obsId), "ObserveEvent.ScitargetAcqStart", prefix),
                new TestData(sequencerObserveEvent.scitargetAcqEnd(obsId), "ObserveEvent.ScitargetAcqEnd", prefix),
                new TestData(sequencerObserveEvent.observationStart(obsId), "ObserveEvent.ObservationStart", prefix),
                new TestData(sequencerObserveEvent.observationEnd(obsId), "ObserveEvent.ObservationEnd", prefix),
                new TestData(sequencerObserveEvent.observeStart(obsId), "ObserveEvent.ObserveStart", prefix),
                new TestData(sequencerObserveEvent.observeEnd(obsId), "ObserveEvent.ObserveEnd", prefix)
        ));
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }

    @Test
    public void createObserveEventWithExposureIdParameters__CSW_125() {
        List<TestData> testData = new ArrayList<>(Arrays.asList(
                new TestData(sequencerObserveEvent.exposureStart(ExposureId.apply(exposureId)), "ObserveEvent.ExposureStart", prefix),
                new TestData(sequencerObserveEvent.exposureEnd(ExposureId.apply(exposureId)), "ObserveEvent.ExposureEnd", prefix),
                new TestData(sequencerObserveEvent.readoutEnd(ExposureId.apply(exposureId)), "ObserveEvent.ReadoutEnd", prefix),
                new TestData(sequencerObserveEvent.readoutFailed(ExposureId.apply(exposureId)), "ObserveEvent.ReadoutFailed", prefix),
                new TestData(sequencerObserveEvent.dataWriteStart(ExposureId.apply(exposureId)), "ObserveEvent.DataWriteStart", prefix),
                new TestData(sequencerObserveEvent.dataWriteEnd(ExposureId.apply(exposureId)), "ObserveEvent.DataWriteEnd", prefix),
                new TestData(sequencerObserveEvent.prepareStart(ExposureId.apply(exposureId)), "ObserveEvent.PrepareStart", prefix)
        ));
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }

    @Test
    public void createDowntimeStartObserveEventWithFixedParameterSet__CSW_125() {
        ObserveEvent event = sequencerObserveEvent.downtimeStart(obsId, "infra failure");
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);
        paramSet.add(downTimeParam);

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.DowntimeStart", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    @Test
    public void createObservePausedEvent__CSW_125() {
        ObserveEvent event = sequencerObserveEvent.observePaused();
        Assert.assertEquals("ObserveEvent.ObservePaused", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    @Test
    public void createObserveResumedEvent__CSW_125() {
        ObserveEvent event = sequencerObserveEvent.observeResumed();
        Assert.assertEquals("ObserveEvent.ObserveResumed", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    public static class TestData {
        final ObserveEvent event;
        final String expectedName;
        final Prefix prefix;

        TestData(ObserveEvent event, String expectedName, Prefix prefix) {
            this.event = event;
            this.expectedName = expectedName;
            this.prefix = prefix;
        }
    }

    private void assertEvent(ObserveEvent event, String name) {
        Assert.assertEquals(name, event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }
}
