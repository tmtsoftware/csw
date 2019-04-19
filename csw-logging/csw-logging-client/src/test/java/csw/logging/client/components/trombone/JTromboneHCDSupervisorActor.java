package csw.logging.client.components.trombone;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;

public class JTromboneHCDSupervisorActor {

    public static Behavior<String> behavior(JLoggerFactory loggerFactory) {
        return Behaviors.setup(context -> {
            // DEOPSCSW-316: Improve Logger accessibility for component developers
            final ILogger log = loggerFactory.getLogger(context, JTromboneHCDSupervisorActor.class);
            return Behaviors.receiveMessage(
                    msg -> {
                        switch (msg) {
                            case "trace": log.trace(() -> msg); break;
                            case "debug": log.debug(() -> msg); break;
                            case "info": log.info(() -> msg); break;
                            case "warn": log.warn(() -> msg); break;
                            case "error": log.error(() -> msg); break;
                            case "fatal": log.fatal(() -> msg); break;
                        }
                        return Behaviors.same();
                    });
        });
    }
}