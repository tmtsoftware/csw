package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.ExposureId;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

public class JSequencerObserveEventTest extends JUnitSuite {
    final Prefix prefix = new Prefix(JSubsystem.ESW, "filter.wheel");
    final ObsId obsId = ObsId.apply("2020A-001-123");
    final ExposureId exposureId = ExposureId.fromString("2021A-001-123-TCS-DET-SCI2-1234");
    final Parameter<String> exposureIdParam = ObserveEventKeys.exposureId().set(exposureId.toString());
    final Parameter<String> obsIdParam = ObserveEventKeys.obsId().set(obsId.toString());
    final Parameter<String> downTimeParam = ObserveEventKeys.downTimeReason().set("infra failure");
    final Parameter<Double> pOffsetParam = JKeyType.DoubleKey().make("p", JUnits.arcsec).set(10.0);
    final Parameter<Double> qOffsetParam = JKeyType.DoubleKey().make("q", JUnits.arcsec).set(20.0);
    final SequencerObserveEvent sequencerObserveEvent = new SequencerObserveEvent(prefix);
    String filename = "some/nested/folder/file123.conf";
    Parameter<String> filenameParam = ObserveEventKeys.filename().set(filename);
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
                new TestData(sequencerObserveEvent.exposureStart(exposureId), "ObserveEvent.ExposureStart", prefix),
                new TestData(sequencerObserveEvent.exposureEnd(exposureId), "ObserveEvent.ExposureEnd", prefix),
                new TestData(sequencerObserveEvent.readoutEnd(exposureId), "ObserveEvent.ReadoutEnd", prefix),
                new TestData(sequencerObserveEvent.readoutFailed(exposureId), "ObserveEvent.ReadoutFailed", prefix),
                new TestData(sequencerObserveEvent.prepareStart(exposureId), "ObserveEvent.PrepareStart", prefix),
                new TestData(sequencerObserveEvent.exposureAborted(exposureId), "ObserveEvent.ExposureAborted", prefix)
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
    public void createOffsetStartObserveEventWithFixedParameterSet__CSW_176() {
        ObserveEvent event = sequencerObserveEvent.offsetStart(obsId,10.0, 20.0);
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(pOffsetParam);
        paramSet.add(qOffsetParam);
        paramSet.add(obsIdParam);

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.OffsetStart", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    @Test
    public void createOffsetEndObserveEventWithFixedParameterSet__CSW_176() {
        ObserveEvent event = sequencerObserveEvent.offsetEnd(obsId);
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);

        Set<Parameter<?>> paramSet2 = new HashSet<>(10);

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.OffsetEnd", event.eventName().name());
        Assert.assertEquals(prefix, event.source());

    }

    @Test
    public void createInputRequestStartObserveEventWithFixedParameterSet__CSW_176() {
        ObserveEvent event = sequencerObserveEvent.inputRequestStart(obsId);
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.InputRequestStart", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    @Test
    public void createInputRequestEndObserveEventWithFixedParameterSet__CSW_176() {
        ObserveEvent event = sequencerObserveEvent.inputRequestEnd(obsId);
        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.InputRequestEnd", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    @Test
    public void shouldCreateEventWithExposureIdAndFilename__CSW_118_CSW_119() {
        List<JIRDetectorEventTest.TestData> testData = new ArrayList<>(Arrays.asList(
                new JIRDetectorEventTest.TestData(sequencerObserveEvent.dataWriteStart(exposureId, filename), "ObserveEvent.DataWriteStart"),
                new JIRDetectorEventTest.TestData(sequencerObserveEvent.dataWriteEnd(exposureId, filename), "ObserveEvent.DataWriteEnd")));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(filenameParam);

        for (JIRDetectorEventTest.TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
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
