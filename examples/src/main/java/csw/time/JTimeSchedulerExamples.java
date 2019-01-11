package csw.time;

import akka.actor.ActorSystem;
import csw.time.client.api.TimeServiceScheduler;
import csw.time.client.TimeServiceSchedulerFactory;

public class JTimeSchedulerExamples {
    private ActorSystem actorSystem;

    public JTimeSchedulerExamples(ActorSystem actorSystem){
        this.actorSystem = actorSystem;
    }

    //#create-scheduler
    // create time service scheduler using the factory method
    private TimeServiceScheduler scheduler = TimeServiceSchedulerFactory.make(actorSystem);
    //#create-scheduler

}
