package csw.logging.client.components.iris;

import akka.actor.AbstractActor;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;

public class JIrisSupervisorActor extends AbstractActor {

    // DEOPSCSW-316: Improve Logger accessibility for component developers
    private ILogger log = new JLoggerFactory("jIRIS").getLogger(context(), getClass());

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg -> msg.equals("trace"), msg -> log.trace(() -> msg))
                .match(String.class, msg -> msg.equals("debug"), msg -> log.debug(() -> msg))
                .match(String.class, msg -> msg.equals("info"), msg -> log.info(() -> msg))
                .match(String.class, msg -> msg.equals("warn"), msg -> log.warn(() -> msg))
                .match(String.class, msg -> msg.equals("error"), msg -> log.error(() -> msg))
                .match(String.class, msg -> msg.equals("fatal"), msg -> log.fatal(() -> msg))
                .build();
    }
}
