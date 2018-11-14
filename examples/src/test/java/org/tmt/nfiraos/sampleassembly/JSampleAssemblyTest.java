package org.tmt.nfiraos.sampleassembly;

import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.models.AkkaLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

//#intro
public class JSampleAssemblyTest extends JUnitSuite {
    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));
//#intro

    //#setup
    @BeforeClass
    public static void setup() {
        testKit.spawnStandalone(com.typesafe.config.ConfigFactory.load("JSampleAssemblyStandalone.conf"));
    }
//#setup

    //#locate
    @Test
    public void testAssemblyShouldBeLocatableUsingLocationService() throws ExecutionException, InterruptedException {
        Connection.AkkaConnection connection = new Connection.AkkaConnection(new ComponentId("JSampleAssembly", JComponentType.Assembly));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().get();

        Assert.assertEquals(location.connection(), connection);
    }
    //#locate
}
