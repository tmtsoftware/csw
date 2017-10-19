package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerImmutable;

public class JComponentImmutableActorLogger {

    public static <T> Behavior<T> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {
            ILogger log = JComponentLoggerImmutable.getLogger(ctx, componentName, JComponentImmutableActorLogger.class);
            return Actor.same();
        });
    }

}