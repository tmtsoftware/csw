package csw.logging.core.utils;

import akka.actor.AbstractActor;
import csw.logging.core.javadsl.ILogger;
import csw.logging.core.javadsl.JGenericLoggerFactory;

public class JGenericActor extends AbstractActor {
    private ILogger logger = JGenericLoggerFactory.getLogger(context(), getClass());

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg -> msg.equals("trace"), msg -> logger.trace(() -> msg))
                .match(String.class, msg -> msg.equals("debug"), msg -> logger.debug(() -> msg))
                .match(String.class, msg -> msg.equals("info"), msg -> logger.info(() -> msg))
                .match(String.class, msg -> msg.equals("warn"), msg -> logger.warn(() -> msg))
                .match(String.class, msg -> msg.equals("error"), msg -> logger.error(() -> msg))
                .match(String.class, msg -> msg.equals("fatal"), msg -> logger.fatal(() -> msg))
                .build();
    }
}
