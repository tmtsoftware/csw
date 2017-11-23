package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

//#component-logger
public class JImmutableSample {

    public <ComponentDomainMessage> Behavior<ComponentDomainMessage> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {

            //JImmutableSample.class will appear against class tag in log statements
            ILogger log = new JLoggerFactory(componentName).getLogger(ctx, getClass());

            return Actor.same();
        });
    }

}
//#component-logger