package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.*;
import csw.params.javadsl.JKeyType;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

public class JOpticalDetectorEventTest extends JUnitSuite {
    Prefix sourcePrefix = new Prefix(JSubsystem.ESW, "filter.wheel");
    ObsId obsId = ObsId.apply("2020A-001-123");

    ExposureId exposureId = ExposureId.apply("2022A-001-123-IRIS-IMG-DRK1-0023");
    String detector = "optical-detector";
    Parameter<String> obsIdParam = JKeyType.StringKey().make("obsId").set(obsId.toString());
    Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());

    @Test
    public void shouldCreateIrDetectorObserveEventWithObsId__CSW_118_CSW_119() {
        List<TestData> testData = new ArrayList(Arrays.asList(
                new TestData(OpticalDetectorEvent.observeStart(sourcePrefix, obsId), "ObserveEvent.ObserveStart"),
                new TestData(OpticalDetectorEvent.observeEnd(sourcePrefix, obsId), "ObserveEvent.ObserveEnd")
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
                new TestData(OpticalDetectorEvent.prepareStart(sourcePrefix, obsId, exposureId), "ObserveEvent.PrepareStart"),
                new TestData(OpticalDetectorEvent.exposureStart(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureStart"),
                new TestData(OpticalDetectorEvent.exposureEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureEnd"),
                new TestData(OpticalDetectorEvent.readoutEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.ReadoutEnd"),
                new TestData(OpticalDetectorEvent.readoutFailed(sourcePrefix, obsId, exposureId), "ObserveEvent.ReadoutFailed"),
                new TestData(OpticalDetectorEvent.dataWriteStart(sourcePrefix, obsId, exposureId), "ObserveEvent.DataWriteStart"),
                new TestData(OpticalDetectorEvent.dataWriteEnd(sourcePrefix, obsId, exposureId), "ObserveEvent.DataWriteEnd"),
                new TestData(OpticalDetectorEvent.exposureAborted(sourcePrefix, obsId, exposureId), "ObserveEvent.ExposureAborted")
        ));

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

        ObserveEvent event = OpticalDetectorEvent.exposureState(
                sourcePrefix,
                exposureId,
                true,
                false,
                true,
                "",
                JOperationalState.READY()
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.OpticalDetectorExposureState", event.eventName().name());
        Assert.assertEquals(sourcePrefix, event.source());
    }

    @Test
    public void shouldCreateOpticalDetectorExposureDataEvent__CSW_118_CSW_119() {
        long exposureTime = 1000L;
        long remainingExposureTime = 20L;
        Set<Parameter<?>> paramSet = getParamSetForExposureDataEvent(exposureTime, remainingExposureTime);

        ObserveEvent event = OpticalDetectorEvent.exposureData(
                sourcePrefix,
                exposureId,
                exposureTime,
                remainingExposureTime
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.OpticalDetectorExposureData", event.eventName().name());
        Assert.assertEquals(sourcePrefix, event.source());
    }

    private Set<Parameter<?>> getParamSetForExposureStateEvent() {
        Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());
        Parameter<Boolean> exposureInProgress = JKeyType.BooleanKey().make("exposureInProgress").set(true);
        Parameter<Boolean> abortInProgress = JKeyType.BooleanKey().make("abortInProgress").set(false);
        Parameter<Boolean> isAborted = JKeyType.BooleanKey().make("isAborted").set(true);
        Parameter<String> errorMessage = JKeyType.StringKey().make("errorMessage").set("");
        HashSet<Choice> operationalStateChoices = ObserveEventUtil.getOperationalStateChoices();
        Parameter<Choice> operationalState = JKeyType.ChoiceKey().make("operationalState",  Choices.fromChoices(operationalStateChoices)).set(new Choice(JOperationalState.READY().entryName()));


        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(exposureInProgress);
        paramSet.add(abortInProgress);
        paramSet.add(isAborted);
        paramSet.add(errorMessage);
        paramSet.add(operationalState);
        return paramSet;
    }


    private Set<Parameter<?>> getParamSetForExposureDataEvent(long exposureTime, long remainingExposureTime) {
        Parameter<Long> exposureTimeParam = JKeyType.LongKey().make("exposureTime").set(exposureTime);
        Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());
        Parameter<Long> remainingExposureTimeParam = JKeyType.LongKey().make("remainingExposureTime").set(remainingExposureTime);

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(exposureTimeParam);
        paramSet.add(remainingExposureTimeParam);
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

