package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.core.models.ExposureId;
import csw.params.javadsl.JKeyType;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

import static csw.prefix.javadsl.JSubsystem.DMS;

public class JDMSObserveEventTest extends JUnitSuite {
    Prefix prefix = new Prefix(DMS, "Metadata");
    ExposureId exposureId = ExposureId.fromString("2022A-001-123-IRIS-IMG-DRK1-0023");
    Parameter<String> exposureIdParam = JKeyType.StringKey().make("exposureId").set(exposureId.toString());

    @Test
    public void shouldCreateDMSObserveEventWithExposureId__CSW_156() {
        List<JDMSObserveEventTest.TestData> testData = new ArrayList<>(Arrays.asList(
                new JDMSObserveEventTest.TestData(DMSObserveEvent.metadataAvailable(exposureId), "ObserveEvent.MetadataAvailable"),
                new JDMSObserveEventTest.TestData(DMSObserveEvent.exposureAvailable(exposureId), "ObserveEvent.ExposureAvailable")));

        Set<Parameter<?>> paramSet = new HashSet<>(10);
        paramSet.add(exposureIdParam);

        for (JDMSObserveEventTest.TestData data : testData) {
            assertEvent(data.event, data.expectedName);
            Assert.assertEquals(paramSet, data.event.jParamSet());
        }
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
        Assert.assertEquals(prefix, event.source());
    }

}
