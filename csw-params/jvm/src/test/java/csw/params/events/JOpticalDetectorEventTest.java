/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.Choice;
import csw.params.core.models.Choices;
import csw.params.core.models.ExposureId;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class JOpticalDetectorEventTest {
    final Prefix sourcePrefix = new Prefix(JSubsystem.ESW, "filter.wheel");
    final ObsId obsId = ObsId.apply("2020A-001-123");

    ExposureId exposureId = ExposureId.fromString("2022A-001-123-IRIS-IMG-DRK1-0023");
    Parameter<String> obsIdParam = JKeyType.StringKey().make("obsId").set(obsId.toString());
    Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());
    String filename = "some/nested/folder/file123.conf";
    Parameter<String> filenameParam = JKeyType.StringKey().make("filename").set(filename);

    @Test
    public void shouldOpticalDetectorEventWithObsId__CSW_118_CSW_119() {
        List<TestData> testData = new ArrayList<>(Arrays.asList(
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
    public void shouldOpticalDetectorEventWithoutObsId__CSW_118_CSW_119() {
        List<TestData> testData = new ArrayList<>(Arrays.asList(
                new TestData(OpticalDetectorEvent.observeStart(sourcePrefix), "ObserveEvent.ObserveStart"),
                new TestData(OpticalDetectorEvent.observeEnd(sourcePrefix), "ObserveEvent.ObserveEnd")
        ));

        Set<Parameter<?>> paramSet = new HashSet<>(10);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }


    @Test
    public void shouldOpticalDetectorEventWithExposureId__CSW_118_CSW_119() {
        List<TestData> testData = new ArrayList<>(Arrays.asList(
                new TestData(OpticalDetectorEvent.prepareStart(sourcePrefix, exposureId), "ObserveEvent.PrepareStart"),
                new TestData(OpticalDetectorEvent.exposureStart(sourcePrefix, exposureId), "ObserveEvent.ExposureStart"),
                new TestData(OpticalDetectorEvent.exposureEnd(sourcePrefix, exposureId), "ObserveEvent.ExposureEnd"),
                new TestData(OpticalDetectorEvent.readoutEnd(sourcePrefix, exposureId), "ObserveEvent.ReadoutEnd"),
                new TestData(OpticalDetectorEvent.readoutFailed(sourcePrefix, exposureId), "ObserveEvent.ReadoutFailed"),
                new TestData(OpticalDetectorEvent.exposureAborted(sourcePrefix, exposureId), "ObserveEvent.ExposureAborted")
        ));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);

        for (TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
    }

    @Test
    public void shouldOpticalDetectorEventWithExposureIdAndFilename__CSW_118_CSW_119() {
        List<JIRDetectorEventTest.TestData> testData = new ArrayList<>(Arrays.asList(
                new JIRDetectorEventTest.TestData(OpticalDetectorEvent.dataWriteStart(sourcePrefix, exposureId, filename), "ObserveEvent.DataWriteStart"),
                new JIRDetectorEventTest.TestData(OpticalDetectorEvent.dataWriteEnd(sourcePrefix, exposureId, filename), "ObserveEvent.DataWriteEnd")));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(filenameParam);

        for (JIRDetectorEventTest.TestData data : testData) {
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
        int coaddsInExposure = 4;
        int coaddsDone = 2;
        Set<Parameter<?>> paramSet = getParamSetForExposureDataEvent(exposureTime, remainingExposureTime, coaddsInExposure, coaddsDone);
        ObserveEvent event = OpticalDetectorEvent.exposureData(
                sourcePrefix,
                exposureId,
                coaddsInExposure,
                coaddsDone,
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
        Parameter<Choice> operationalState = JKeyType.ChoiceKey().make("operationalState", Choices.fromChoices(operationalStateChoices)).set(new Choice(JOperationalState.READY().entryName()));


        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(exposureInProgress);
        paramSet.add(abortInProgress);
        paramSet.add(isAborted);
        paramSet.add(errorMessage);
        paramSet.add(operationalState);
        return paramSet;
    }


    private Set<Parameter<?>> getParamSetForExposureDataEvent(long exposureTime, long remainingExposureTime, int coaddsInExposure, int coaddsDone) {
        Parameter<Long> exposureTimeParam = JKeyType.LongKey().make("exposureTime", JUnits.millisecond).set(exposureTime);
        Parameter<Integer> coaddsInExposureParam = JKeyType.IntKey().make("coaddsInExposure").set(coaddsInExposure);
        Parameter<Integer> coaddsDoneParam = JKeyType.IntKey().make("coaddsDone").set(coaddsDone);
        Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());
        Parameter<Long> remainingExposureTimeParam = JKeyType.LongKey().make("remainingExposureTime", JUnits.millisecond).set(remainingExposureTime);

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);
        paramSet.add(exposureTimeParam);
        paramSet.add(coaddsInExposureParam);
        paramSet.add(coaddsDoneParam);
        paramSet.add(remainingExposureTimeParam);
        return paramSet;
    }

    public static class TestData {
        final ObserveEvent event;
        final String expectedName;

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

