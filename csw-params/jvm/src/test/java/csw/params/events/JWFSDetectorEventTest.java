package csw.params.events;

import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

public class JWFSDetectorEventTest extends JUnitSuite {
    String sourcePrefix = "ESW.filter.wheel";
    Prefix prefix = new Prefix(JSubsystem.ESW, "filter.wheel");

    @Test
    public void shouldCreatePublishSuccessEvent__CSW_118() {
        ObserveEvent event = WFSDetectorEvent.publishSuccess(sourcePrefix);

        Assert.assertEquals(prefix, event.source());
        Assert.assertEquals("PublishSuccess", event.eventName().name());
    }

    @Test
    public void shouldCreatePublishFailEvent__CSW_118() {
        ObserveEvent event = WFSDetectorEvent.publishFail(sourcePrefix);

        Assert.assertEquals(prefix, event.source());
        Assert.assertEquals("PublishFail", event.eventName().name());
    }
}

