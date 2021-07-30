package csw.params.events;

import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

public class JWFSDetectorEventTest extends JUnitSuite {
    Prefix sourcePrefix = new Prefix(JSubsystem.ESW, "filter.wheel");

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
}

