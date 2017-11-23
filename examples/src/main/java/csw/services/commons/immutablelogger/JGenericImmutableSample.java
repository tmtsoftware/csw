package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLoggerFactory;

//#generic-logger
public class JGenericImmutableSample {

    public <ComponentDomainMessage> Behavior<ComponentDomainMessage> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {

            //JGenericImmutableSample.class will appear against class tag in log statements
            ILogger log =  JGenericLoggerFactory.getLogger(ctx, getClass());

            return Actor.same();
        });
    }

}
//#generic-logger