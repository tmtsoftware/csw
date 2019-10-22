package example.alarm;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import csw.alarm.api.javadsl.IAlarmService;
import csw.alarm.client.AlarmServiceFactory;
import csw.location.api.javadsl.ILocationService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import csw.alarm.api.javadsl.JAlarmSeverity;
import static csw.alarm.models.Key.AlarmKey;
import static csw.params.javadsl.JSubsystem.NFIRAOS;

public class JAlarmServiceClientExampleApp {

    private ActorSystem<SpawnProtocol.Command> actorSystem;
    private ILocationService jLocationService;

    public JAlarmServiceClientExampleApp(akka.actor.typed.ActorSystem<SpawnProtocol.Command> actorSystem, ILocationService locationService) throws ExecutionException, InterruptedException {
        this.actorSystem = actorSystem;
        this.jLocationService = locationService;

    }

    //#create-java-api
    // create alarm client using host and port of alarm server
    IAlarmService jclientAPI1 = new AlarmServiceFactory().jMakeClientApi("localhost", 5227, actorSystem);

    // create alarm client using location service
    IAlarmService jclientAPI2 = new AlarmServiceFactory().jMakeClientApi(jLocationService, actorSystem);
    //#create-java-api

    //#setSeverity-java
    private AlarmKey alarmKey = new AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm");
    Future<Done> doneF = jclientAPI1.setSeverity(alarmKey, JAlarmSeverity.Okay());
    //#setSeverity-java
}
