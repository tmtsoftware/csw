package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLoggerImmutable;

//#generic-logger
public class JGenericImmutableSample {

    public static <ComponentDomainMessage> Behavior<ComponentDomainMessage> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {

            //JGenericImmutableSample.class will appear against class tag in log statements
            ILogger log = JGenericLoggerImmutable.getLogger(ctx, JGenericImmutableSample.class);

            return Actor.same();
        });
    }

}
//#generic-logger