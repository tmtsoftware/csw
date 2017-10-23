package csw.services.commons.commonlogger;

import akka.typed.javadsl.ActorContext;
import csw.services.logging.javadsl.JCommonComponentLoggerMutableActor;

//#common-component-logger-mutable-actor
//JMutableActorSample is used for mutable actor java class only
public abstract class JMutableActorSample extends JCommonComponentLoggerMutableActor<Object> {
    public JMutableActorSample(ActorContext<Object> ctx) {
        super(ctx);
    }

    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#common-component-logger-mutable-actor