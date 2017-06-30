package csw.services.logging.utils;

import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLoggerActor;

public class JGenericActor extends JGenericLoggerActor {
    private ILogger logger = getLogger();

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
