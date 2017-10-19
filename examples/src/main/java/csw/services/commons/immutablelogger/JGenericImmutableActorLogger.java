package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerImmutable;
import csw.services.logging.javadsl.JGenericLoggerImmutable;

public class JGenericImmutableActorLogger {

    public static <T> Behavior<T> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {

            ILogger log = JGenericLoggerImmutable.getLogger(ctx, JGenericImmutableActorLogger.class);

            return Actor.same();
        });
    }

}