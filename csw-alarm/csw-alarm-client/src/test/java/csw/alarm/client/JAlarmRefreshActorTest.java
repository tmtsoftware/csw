package csw.alarm.client;

import akka.Done;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import csw.alarm.api.javadsl.JAlarmSeverity;
import csw.alarm.models.AutoRefreshSeverityMessage;
import csw.alarm.models.AutoRefreshSeverityMessage.AutoRefreshSeverity;
import csw.alarm.models.AutoRefreshSeverityMessage.CancelAutoRefresh;
import csw.alarm.models.Key.AlarmKey;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

//CSW-83:Alarm models should take prefix
public class JAlarmRefreshActorTest {
    private final ActorSystem<SpawnProtocol.Command> typedSystem = ActorSystem.apply(SpawnProtocol.create(), "SpawnProtocolGuardian");

    // DEOPSCSW-507: Auto-refresh utility for component developers
    @Test
    public void should_refresh_severity__DEOPSCSW_507() {
        AlarmKey alarmKey = new AlarmKey(Prefix.apply(JSubsystem.NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm");
        TestProbe<String> probe = TestProbe.create(typedSystem);
        String refreshMsg = "severity refreshed";

        ActorRef<AutoRefreshSeverityMessage> ref = AlarmRefreshActorFactory.jMake((key, severity) -> CompletableFuture.supplyAsync(() -> {
            probe.ref().tell(refreshMsg);
            return Done.done();
        }), Duration.ofMillis(200), typedSystem);

        ref.tell(new AutoRefreshSeverity(alarmKey, JAlarmSeverity.Major));
        probe.expectMessage(refreshMsg);
        probe.expectNoMessage(Duration.ofMillis(190));
        probe.expectMessage(Duration.ofMillis(50), refreshMsg);

        ref.tell(new CancelAutoRefresh(alarmKey));
        probe.expectNoMessage(Duration.ofMillis(210));
    }

}
