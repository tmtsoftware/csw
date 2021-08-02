package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.*;
import csw.params.javadsl.JKeyType;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

import static csw.prefix.javadsl.JSubsystem.ESW;

public class JIRDetectorEventTest extends JUnitSuite {
    Prefix sourcePrefix = new Prefix(ESW, "filter.wheel");
    ObsId obsId = ObsId.apply("2020A-001-123");
    String detector = "ir-detector";
    Parameter<String> obsIdParam = JKeyType.StringKey().make("obsId").set(obsId.toString());
    ExposureIdType exposureId = ExposureId.apply("2022A-001-123-IRIS-IMG-DRK1-0023");
    Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());

    @Test
    public void shouldCreateIrDetectorObserveEventWithObsId__CSW_118_CSW_119() {
        List<TestData> testData = new ArrayList(Arrays.asList(
                new TestData(IRDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveEvent.ObserveStart"),
                new TestData(IRDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEvent.ObserveEnd")
        ));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }


    @Test
    public void shouldCreateIrDetectorObserveEventWithObsIdAndExposureId__CSW_118_CSW_119() {
        List<TestData> testData = new ArrayList(Arrays.asList(
                new TestData(IRDetectorEvent.exposureStart(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureStart"),
                new TestData(IRDetectorEvent.exposureEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureEnd"),
                new TestData(IRDetectorEvent.readoutEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.ReadoutEnd"),
                new TestData(IRDetectorEvent.readoutFailed(sourcePrefix, obsId, exposureId), "ObserveEvent.ReadoutFailed"),
                new TestData(IRDetectorEvent.dataWriteStart(sourcePrefix, obsId, exposureId), "ObserveEvent.DataWriteStart"),
                new TestData(IRDetectorEvent.dataWriteEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.DataWriteEnd"),
                new TestData(IRDetectorEvent.exposureAborted(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureAborted")));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(obsIdParam);
        paramSet.add(exposureIdParam);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }

    @Test
    public void shouldCreateIrDetectorExposureStateEvent__CSW_118_CSW_119() {
        Set<Parameter<?>> paramSet = getParamSetForExposureStateEvent();

        ObserveEvent event = IRDetectorEvent.exposureState(
                sourcePrefix,
                exposureId,
                true,
                false,
                true,
                "",
                JOperationalState.BUSY()
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.IRDetectorExposureState", event.eventName().name());
        Assert.assertEquals(sourcePrefix, event.source());
    }

    @Test
    public void shouldCreateIrDetectorExposureDataEvent__CSW_118_CSW_119() {
        int readsInRamp = 1;
        int readsComplete = 20;
        int rampsInExposure = 40;
        int rampsComplete = 50;
        long exposureTime = 1000L;
        long remainingExposureTime = 20L;
        Set<Parameter<?>> paramSet = getParamSetForExposureDataEvent(readsInRamp, readsComplete, rampsInExposure, rampsComplete, exposureTime, remainingExposureTime);

        ObserveEvent event = IRDetectorEvent.exposureData(
                sourcePrefix,
                exposureId,
                readsInRamp,
                readsComplete,
                rampsInExposure,
                rampsComplete,
                exposureTime,
                remainingExposureTime
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.IRDetectorExposureData", event.eventName().name());
        Assert.assertEquals(sourcePrefix, event.source());
    }

    private Set<Parameter<?>> getParamSetForExposureDataEvent(int readsInRamp, int readsComplete, int rampsInExposure, int rampsComplete, long exposureTime, long remainingExposureTime) {
        Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());
        Parameter<Integer> readsInRampParam = JKeyType.IntKey().make("readsInRamp").set(readsInRamp);
        Parameter<Integer> readsCompleteParam = JKeyType.IntKey().make("readsComplete").set(readsComplete);
        Parameter<Integer> rampsInExposureParam = JKeyType.IntKey().make("rampsInExposure").set(rampsInExposure);
        Parameter<Integer> rampsCompleteParam = JKeyType.IntKey().make("rampsComplete").set(rampsComplete);
        Parameter<Long> exposureTimeParam = JKeyType.LongKey().make("exposureTime").set(exposureTime);
        Parameter<Long> remainingExposureTimeParam = JKeyType.LongKey().make("remainingExposureTime").set(remainingExposureTime);

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(readsInRampParam);
        paramSet.add(readsCompleteParam);
        paramSet.add(rampsInExposureParam);
        paramSet.add(rampsCompleteParam);
        paramSet.add(exposureTimeParam);
        paramSet.add(remainingExposureTimeParam);
        return paramSet;
    }

    private Set<Parameter<?>> getParamSetForExposureStateEvent() {
        Parameter<Boolean> exposureInProgress = JKeyType.BooleanKey().make("exposureInProgress").set(true);
        Parameter<Boolean> abortInProgress = JKeyType.BooleanKey().make("abortInProgress").set(false);
        Parameter<Boolean> isAborted = JKeyType.BooleanKey().make("isAborted").set(true);
        Parameter<String> errorMessage = JKeyType.StringKey().make("errorMessage").set("");
        HashSet<Choice> operationalStateChoices = ObserveEventUtil.getOperationalStateChoices();
        Parameter<Choice> operationalState = JKeyType.ChoiceKey().make("operationalState", Choices.fromChoices(operationalStateChoices)).set(new Choice(JOperationalState.BUSY().entryName()));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
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
        Assert.assertEquals(sourcePrefix, event.source());
    }
}

