package org.tmt.nfiraos.samplehcd;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.util.Timeout;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.event.api.javadsl.IEventService;
import csw.event.api.javadsl.IEventSubscriber;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.models.AkkaLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.*;
import csw.params.commands.CommandName;
import csw.params.commands.CommandResponse;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.ObsId;
import csw.params.events.Event;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

//#setup
public class JSampleHcdTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));


    @BeforeClass
    public static void setup() {
        testKit.spawnStandalone(com.typesafe.config.ConfigFactory.load("JSampleHcdStandalone.conf"));
    }

    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testHCDShouldBeLocatableUsingLocationService() throws ExecutionException, InterruptedException {
        AkkaConnection connection = new AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.NFIRAOS, "JSampleHcd"), JComponentType.HCD));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().orElseThrow();

        Assert.assertEquals(connection, location.connection());
    }
//#setup

    //#subscribe
    @Test
    public void testShouldBeAbleToSubscribeToHCDEvents() throws InterruptedException {
        EventKey counterEventKey = new EventKey(Prefix.apply("nfiraos.JSampleHcd"), new EventName("HcdCounter"));
        Key<Integer> hcdCounterKey = JKeyType.IntKey().make("counter");

        IEventService eventService = testKit.jEventService();
        IEventSubscriber subscriber = eventService.defaultSubscriber();

        ArrayList<Event> subscriptionEventList = new ArrayList<>();
        subscriber.subscribeCallback(Set.of(counterEventKey), subscriptionEventList::add);

        // Sleep for 5 seconds, to allow HCD to publish events
        Thread.sleep(5000);

        // Event publishing period is 2 seconds.
        // Expecting 4 events: first event on subscription (-1)
        // and 3 more events 1, 2, and 3.
        Assert.assertEquals(4, subscriptionEventList.size());

        // extract counter values to a List for comparison
        List<Integer> counterList = subscriptionEventList.stream()
                .map(ev -> {
                    SystemEvent sysEv = ((SystemEvent) ev);
                    if (sysEv.contains(hcdCounterKey)) {
                        return sysEv.parameter(hcdCounterKey).head();
                    } else {
                        return -1;
                    }
                })
                .collect(Collectors.toList());

        // we don't know exactly how long HCD has been running when this test runs,
        // so we don't know what the first value will be,
        // but we know we should get three consecutive numbers
        int counter0 = counterList.get(0);
        List<Integer> expectedCounterList = Arrays.asList(counter0, 1, 2, 3);

        Assert.assertEquals(expectedCounterList, counterList);
    }
    //#subscribe

    //#submitAndWait
    private ActorSystem<SpawnProtocol.Command> typedActorSystem = testKit.actorSystem();

    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testShouldBeAbleToSendSleepCommandToHCD() throws ExecutionException, InterruptedException, TimeoutException {

        // Construct Setup command
        Key<Long> sleepTimeKey = JKeyType.LongKey().make("SleepTime");
        Parameter<Long> sleepTimeParam = sleepTimeKey.set(5000L).withUnits(JUnits.millisecond);

        Setup setupCommand = new Setup(Prefix.apply(JSubsystem.CSW, "move"), new CommandName(("sleep")), Optional.of(new ObsId("2018A-001"))).add(sleepTimeParam);

        Timeout commandResponseTimeout = new Timeout(10, TimeUnit.SECONDS);

        AkkaConnection connection = new AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.NFIRAOS, "JSampleHcd"), JComponentType.HCD));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().orElseThrow();

        ICommandService hcd = CommandServiceFactory.jMake(location, typedActorSystem);

        CommandResponse.SubmitResponse result = hcd.submitAndWait(setupCommand, commandResponseTimeout).get(10, TimeUnit.SECONDS);
        Assert.assertTrue(result instanceof CommandResponse.Completed);
    }
    //#submitAndWait

    //#exception
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testShouldGetExecutionExceptionIfSubmitTimeoutIsTooSmall() throws ExecutionException, InterruptedException {

        // Construct Setup command
        Key<Long> sleepTimeKey = JKeyType.LongKey().make("SleepTime");
        Parameter<Long> sleepTimeParam = sleepTimeKey.set(5000L).withUnits(JUnits.millisecond);

        Setup setupCommand = new Setup(Prefix.apply(JSubsystem.CSW, "move"), new CommandName("sleep"), Optional.of(new ObsId("2018A-001"))).add(sleepTimeParam);

        Timeout commandResponseTimeout = new Timeout(1, TimeUnit.SECONDS);

        AkkaConnection connection = new AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.NFIRAOS, "JSampleHcd"), JComponentType.HCD));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().orElseThrow();

        ICommandService hcd = CommandServiceFactory.jMake(location, typedActorSystem);

        thrown.expect(ExecutionException.class);
        hcd.submitAndWait(setupCommand, commandResponseTimeout).get();
    }
//#exception
}
