package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

import static csw.params.events.JIRDetectorEvent.*;

public class JIRDetectorEventTest extends JUnitSuite {
    String sourcePrefix = "ESW.filter.wheel";
    Prefix prefix = new Prefix(JSubsystem.ESW, "filter.wheel");
    ObsId obsId = new ObsId("someObsId");
    String exposureId = "some-exposure-id";
    String detector = "ir-detector";
    Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId);
    Parameter<String> obsIdParam = JKeyType.StringKey().make("obsId").set(obsId.obsId());

    @Test
    public void shouldCreateIrDetectorObserveEventWithObsId__CSW_118() {
        List<TestData> testData = new ArrayList(Arrays.asList(
                new TestData(ObserveStart().create(sourcePrefix, obsId), "ObserveStart"),
                new TestData(ObserveEnd().create(sourcePrefix, obsId), "ObserveEnd")
        ));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }


    @Test
    public void shouldCreateIrDetectorObserveEventWithObsIdAndExposureId__CSW_118() {
        List<TestData> testData = new ArrayList(Arrays.asList(
                new TestData(ExposureStart().create(sourcePrefix, obsId, exposureId), "ExposureStart"),
                new TestData(ExposureEnd().create(sourcePrefix, obsId, exposureId), "ExposureEnd"),
                new TestData(ReadoutEnd().create(sourcePrefix, obsId, exposureId), "ReadoutEnd"),
                new TestData(ReadoutFailed().create(sourcePrefix, obsId, exposureId), "ReadoutFailed"),
                new TestData(DataWriteStart().create(sourcePrefix, obsId, exposureId), "DataWriteStart"),
                new TestData(DataWriteEnd().create(sourcePrefix, obsId, exposureId), "DataWriteEnd"),
                new TestData(ExposureAborted().create(sourcePrefix, obsId, exposureId), "ExposureAborted")));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);
        paramSet.add(exposureIdParam);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }

    @Test
    public void shouldCreateIrDetectorExposureStateEvent__CSW_118() {
        Set<Parameter<?>> paramSet = getParamSetForExposureStateEvent();

        ObserveEvent event = JIRDetectorEvent.IRDetectorExposureState().create(
                sourcePrefix,
                obsId,
                detector,
                true,
                false,
                true,
                "",
                OperationalState.BUSY()
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("IRDetectorExposureState", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    @Test
    public void shouldCreateIrDetectorExposureDataEvent__CSW_118() {
        int readsInRamp = 1;
        int readsComplete = 20;
        int rampsInExposure = 40;
        int rampsComplete = 50;
        long exposureTime = 1000L;
        long remainingExposureTime = 20L;
        Set<Parameter<?>> paramSet = getParamSetForExposureDataEvent(readsInRamp, readsComplete, rampsInExposure, rampsComplete, exposureTime, remainingExposureTime);

        ObserveEvent event = JIRDetectorEvent.IRDetectorExposureData().create(
                sourcePrefix,
                obsId,
                detector,
                readsInRamp,
                readsComplete,
                rampsInExposure,
                rampsComplete,
                exposureTime,
                remainingExposureTime
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("IRDetectorExposureData", event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }

    private Set<Parameter<?>> getParamSetForExposureDataEvent(int readsInRamp, int readsComplete, int rampsInExposure, int rampsComplete, long exposureTime, long remainingExposureTime) {
        Parameter<String> detectorParam = JKeyType.StringKey().make("detector").set(detector);
        Parameter<Integer> readsInRampParam = JKeyType.IntKey().make("readsInRamp").set(readsInRamp);
        Parameter<Integer> readsCompleteParam = JKeyType.IntKey().make("readsComplete").set(readsComplete);
        Parameter<Integer> rampsInExposureParam = JKeyType.IntKey().make("rampsInExposure").set(rampsInExposure);
        Parameter<Integer> rampsCompleteParam = JKeyType.IntKey().make("rampsComplete").set(rampsComplete);
        Parameter<Long> exposureTimeParam = JKeyType.LongKey().make("exposureTime").set(exposureTime);
        Parameter<Long> remainingExposureTimeParam = JKeyType.LongKey().make("remainingExposureTime").set(remainingExposureTime);

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);
        paramSet.add(detectorParam);
        paramSet.add(readsInRampParam);
        paramSet.add(readsCompleteParam);
        paramSet.add(rampsInExposureParam);
        paramSet.add(rampsCompleteParam);
        paramSet.add(exposureTimeParam);
        paramSet.add(remainingExposureTimeParam);
        return paramSet;
    }

    private Set<Parameter<?>> getParamSetForExposureStateEvent() {
        Parameter<String> detectorParam = JKeyType.StringKey().make("detector").set(detector);
        Parameter<Boolean> exposureInProgress = JKeyType.BooleanKey().make("exposureInProgress").set(true);
        Parameter<Boolean> abortInProgress = JKeyType.BooleanKey().make("abortInProgress").set(false);
        Parameter<Boolean> isAborted = JKeyType.BooleanKey().make("isAborted").set(true);
        Parameter<String> errorMessage = JKeyType.StringKey().make("errorMessage").set("");
        Parameter<String> operationalState = JKeyType.StringKey().make("operationalState").set("BUSY");

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);
        paramSet.add(detectorParam);
        paramSet.add(exposureInProgress);
        paramSet.add(abortInProgress);
        paramSet.add(isAborted);
        paramSet.add(errorMessage);
        paramSet.add(operationalState);
        return paramSet;
    }

    public static class TestData {
        ObserveEvent event;
        String expectedName;

        TestData(ObserveEvent event, String expectedName) {
            this.event = event;
            this.expectedName = expectedName;
        }
    }

    private void assertEvent(ObserveEvent event, String name) {
        Assert.assertEquals(name, event.eventName().name());
        Assert.assertEquals(prefix, event.source());
    }
}

