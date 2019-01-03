package csw.time.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.ManualTime;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.internal.adapter.ActorSystemAdapter;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import csw.time.api.Cancellable;
import csw.time.api.TimeServiceScheduler;
import csw.time.api.models.TAITime;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class JTimeServiceSchedulerTest extends JUnitSuite {

    @Rule
    public TestKitJunitResource testKit = new TestKitJunitResource(ManualTime.config());

    private ActorSystem untypedSystem = ActorSystemAdapter.toUntyped(testKit.system());
    private ManualTime manualTime = ManualTime.get(testKit.system());

    private TimeServiceScheduler timeServiceScheduler = TimeServiceSchedulerFactory.make(untypedSystem);
    private TestKit untypedTestKit = new TestKit(untypedSystem);

    @BeforeClass
    public static void setup(){
        TAITime.setOffset(37);
    }

    //------------------------------Scheduling-------------------------------

    // DEOPSCSW-542: Schedule a task to execute in future
    // DEOPSCSW-543: Notification on Scheduled timer expiry
    @Test
    public void should_schedule_task_at_start_time() {
        TestProbe testProbe = new TestProbe(untypedSystem);
        String probeMsg = "some message";

        TAITime idealScheduleTime = new TAITime(TAITime.now().value().plusSeconds(1));

        Runnable task = () -> testProbe.ref().tell(probeMsg, ActorRef.noSender());

        Cancellable cancellable = timeServiceScheduler.scheduleOnce(idealScheduleTime, task);

        manualTime.timePasses(Duration.ofSeconds(1));
        testProbe.expectMsg(probeMsg);
        cancellable.cancel();
    }

    // DEOPSCSW-544: Schedule a task to be executed repeatedly
    // DEOPSCSW-547: Cancel scheduled timers for periodic tasks
    // DEOPSCSW-546: Notification on Scheduled Periodic timer expiry
    @Test
    public void should_schedule_a_task_periodically_at_given_interval() {
        List<String> list = new ArrayList<>();

        Cancellable cancellable = timeServiceScheduler.schedulePeriodically(Duration.ofMillis(100), () -> list.add("x"));

        manualTime.timePasses(Duration.ofMillis(500));
        cancellable.cancel();

        assertEquals(list.size(), 6);
    }

    //DEOPSCSW-545: Start a repeating task with initial offset
    //DEOPSCSW-547: Cancel scheduled timers for periodic tasks
    // DEOPSCSW-546: Notification on Scheduled Periodic timer expiry
    @Test
    public void should_schedule_a_task_periodically_at_given_interval_after_start_time() {
        List<String> list = new ArrayList<>();

        TAITime startTime = new TAITime(TAITime.now().value().plusSeconds(1));

        Cancellable cancellable = timeServiceScheduler.schedulePeriodically(startTime, Duration.ofMillis(100), () -> list.add("x"));

        manualTime.timePasses(Duration.ofSeconds(1));
        assertEquals(list.size(), 1);

        manualTime.timePasses(Duration.ofMillis(500));
        cancellable.cancel();
        assertEquals(list.size(), 6);
    }

    //DEOPSCSW-547: Cancel scheduled timers for single scheduled tasks
    @Test
    public void should_cancel_single_scheduled_task(){
        TestProbe testProbe = new TestProbe(untypedSystem);
        String probeMsg = "some message";
        TAITime idealScheduleTime = new TAITime(TAITime.now().value().plusSeconds(1));

        Runnable task = () -> testProbe.ref().tell(probeMsg, ActorRef.noSender());

        Cancellable cancellable = timeServiceScheduler.scheduleOnce(idealScheduleTime, task);
        cancellable.cancel();
        testProbe.expectNoMessage(FiniteDuration.apply(500, TimeUnit.MILLISECONDS));
    }
}