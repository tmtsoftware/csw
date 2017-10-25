package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerImmutable;

//#component-logger
public class JImmutableSample {

    public static <ComponentDomainMessage> Behavior<ComponentDomainMessage> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {

            //JImmutableSample.class will appear against class tag in log statements
            ILogger log = JComponentLoggerImmutable.getLogger(ctx, componentName, JImmutableSample.class);

            return Actor.same();
        });
    }

}
//#component-logger