package example.tutorial.basic;

import akka.util.Timeout;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.event.api.javadsl.IEventSubscriber;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.models.AkkaLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.AkkaConnection;
import csw.params.commands.CommandResponse.Completed;
import csw.params.commands.CommandResponse.SubmitResponse;
import csw.params.commands.Setup;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static example.tutorial.basic.shared.JSampleInfo.*;

public class JSampleIntegrationTest extends JUnitSuite {

    private static AkkaConnection containerConnection = new AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.Container, "JSampleContainer"),
            JComponentType.Container));
    private static AkkaLocation containerLocation;

    private static AkkaConnection assemblyConnection = new AkkaConnection(
            new ComponentId(Prefix.apply(JSubsystem.ESW, "JSampleAssembly"), JComponentType.Assembly));
    private static AkkaLocation assemblyLocation;

    private static AkkaConnection hcdConnection = new AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.ESW, "JSampleHcd"), JComponentType.HCD));
    private static AkkaLocation hcdLocation;
    private static IEventSubscriber subscriber;
    private static EventKey receivedHcdEvent;
    private Timeout timeout = new Timeout(12, TimeUnit.SECONDS);


    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));


    @BeforeClass
    public static void setup() throws ExecutionException, InterruptedException {
        ILocationService locationService = testKit.jLocationService();
        receivedHcdEvent = EventKey.apply(assemblyConnection.prefix(), EventName.apply("receivedHcdLocation"));

        subscriber = testKit.jEventService().defaultSubscriber();

        testKit.spawnContainer(com.typesafe.config.ConfigFactory.load("JBasicSampleContainer.conf"));

        containerLocation = locationService.resolve(containerConnection, Duration.ofSeconds(10)).get().orElseThrow();
        assemblyLocation = locationService.resolve(assemblyConnection, Duration.ofSeconds(10)).get().orElseThrow();
        hcdLocation = locationService.resolve(hcdConnection, Duration.ofSeconds(10)).get().orElseThrow();
    }

    @Test
    public void testContainerShouldBeLocatableUsingLocationService() {
        Assert.assertEquals(containerConnection, containerLocation.connection());
    }

    @Test
    public void testHCDShouldBeLocatableUsingLocationService() {
        Assert.assertEquals(hcdConnection, hcdLocation.connection());
    }

    @Test
    public void testAssemblyShouldBeLocatableUsingLocationService() {
        Assert.assertEquals(assemblyConnection, assemblyLocation.connection());
    }

    @Test
    public void testAcceptAndSendValidSleepCommand() throws InterruptedException, ExecutionException {
        long sleepTime = 1500L;
        Setup setup = new Setup(testPrefix, sleep, Optional.empty()).add(setSleepTime(sleepTime));

        ICommandService assemblyCS = CommandServiceFactory.jMake(assemblyLocation, testKit.actorSystem());
        Assert.assertNotNull(assemblyCS);

        waitTillHcdIsTracked();

        SubmitResponse sr = assemblyCS.submitAndWait(setup, timeout).get();
        Assert.assertTrue(sr instanceof Completed);
        // Check completed value
        Completed completed = (Completed)sr;
        Long lvalue = completed.result().jGet(resultKey).orElseThrow().head();
        Assert.assertEquals(lvalue.longValue(), sleepTime);
    }

    @Test
    public void testImmediateCommand() throws ExecutionException, InterruptedException {
        Setup setup = new Setup(testPrefix, immediateCommand, Optional.empty());

        ICommandService assemblyCS = CommandServiceFactory.jMake(assemblyLocation, testKit.actorSystem());
        Assert.assertNotNull(assemblyCS);

        SubmitResponse sr = assemblyCS.submitAndWait(setup, timeout).get();
        Assert.assertTrue(sr instanceof Completed);
    }

    @Test
    public void testShortMediumLongCommand() throws ExecutionException, InterruptedException {
        Setup shortSetup = new Setup(testPrefix, shortCommand, Optional.empty());
        Setup mediumSetup = new Setup(testPrefix, mediumCommand, Optional.empty());
        Setup longSetup = new Setup(testPrefix, longCommand, Optional.empty());

        ICommandService assemblyCS = CommandServiceFactory.jMake(assemblyLocation, testKit.actorSystem());
        Assert.assertNotNull(assemblyCS);

        waitTillHcdIsTracked();

        Completed r1 = (Completed)assemblyCS.submitAndWait(shortSetup, timeout).get();
        Assert.assertEquals(r1.result().jGet(resultKey).orElseThrow().head(), shortSleepPeriod);

        Completed r2 = (Completed)assemblyCS.submitAndWait(mediumSetup, timeout).get();
        Assert.assertEquals(r2.result().jGet(resultKey).orElseThrow().head(), mediumSleepPeriod);

        Completed r3 = (Completed)assemblyCS.submitAndWait(longSetup, timeout).get();
        Assert.assertEquals(r3.result().jGet(resultKey).orElseThrow().head(), longSleepPeriod);
    }

    @Test
    public void testAComplexCommand() throws ExecutionException, InterruptedException {
        Setup setup = new Setup(testPrefix, complexCommand, Optional.empty());

        ICommandService assemblyCS = CommandServiceFactory.jMake(assemblyLocation, testKit.actorSystem());
        Assert.assertNotNull(assemblyCS);

        waitTillHcdIsTracked();

        SubmitResponse sr = assemblyCS.submitAndWait(setup, timeout).get();
        Assert.assertTrue(sr instanceof Completed);
    }

    private void waitTillHcdIsTracked() {
        Eventually.eventually(Duration.ofSeconds(5), () -> {
            try {
                Assert.assertFalse(subscriber.get(receivedHcdEvent).get().isInvalid());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
