package csw.alarm;

import akka.Done;
import akka.actor.ActorSystem;
import csw.alarm.api.javadsl.IAlarmService;
import csw.alarm.client.AlarmServiceFactory;
import csw.location.api.javadsl.ILocationService;

import java.util.concurrent.ExecutionException;

import static csw.alarm.api.javadsl.JAlarmSeverity.Okay;
import static csw.alarm.api.models.Key.AlarmKey;
import static csw.params.javadsl.JSubsystem.NFIRAOS;

public class JAlarmServiceClientExampleApp {

    private ActorSystem actorSystem;
    private ILocationService jLocationService;

    public JAlarmServiceClientExampleApp(ActorSystem actorSystem,ILocationService locationService) throws ExecutionException, InterruptedException {
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
    AlarmKey alarmKey = new AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm");

    Done done = jclientAPI1.setSeverity(alarmKey, Okay).get();
    //#setSeverity-java
}
