package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.Choice;
import csw.params.core.models.Choices;
import csw.params.core.models.ExposureId;
import csw.params.core.models.ExposureIdType;
import csw.params.javadsl.JKeyType;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.HashSet;
import java.util.Set;

public class JWFSDetectorEventTest extends JUnitSuite {
    Prefix sourcePrefix = new Prefix(JSubsystem.ESW, "filter.wheel");
    ExposureIdType exposureId = ExposureId.apply("2022A-001-123-IRIS-IMG-DRK1-0023");

    @Test
    public void shouldCreatePublishSuccessEvent__CSW_118_CSW_119() {
        ObserveEvent event = WFSDetectorEvent.publishSuccess(sourcePrefix);

        Assert.assertEquals(sourcePrefix, event.source());
        Assert.assertEquals("ObserveEvent.PublishSuccess", event.eventName().name());
    }

    @Test
    public void shouldCreatePublishFailEvent__CSW_118_CSW_119() {
        ObserveEvent event = WFSDetectorEvent.publishFail(sourcePrefix);

        Assert.assertEquals(sourcePrefix, event.source());
        Assert.assertEquals("ObserveEvent.PublishFail", event.eventName().name());
    }

    @Test
    public void shouldCreateWFSDetectorExposureStateEvent__CSW_118_CSW_119() {
        Set<Parameter<?>> paramSet = getParamSetForExposureStateEvent();

        ObserveEvent event = WFSDetectorEvent.exposureState(
                sourcePrefix,
                exposureId,
                true,
                false,
                true,
                JOperationalState.READY(),
                ""
        );

        Assert.assertEquals(paramSet, event.jParamSet());
        Assert.assertEquals("ObserveEvent.WfsDetectorExposureState", event.eventName().name());
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
}

