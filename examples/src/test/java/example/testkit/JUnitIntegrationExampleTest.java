package example.testkit;

//#junit-testkit
import com.typesafe.config.ConfigFactory;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.Arrays;

public class JUnitIntegrationExampleTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));

    @Test
    public void testSpawningComponentInStandaloneMode() {
        testKit.spawnStandalone(ConfigFactory.load("JSampleHcdStandalone.conf"));

         // ... assertions etc.
    }
}
//#junit-testkit