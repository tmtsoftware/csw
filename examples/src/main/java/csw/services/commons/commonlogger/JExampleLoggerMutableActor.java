package csw.services.commons.commonlogger;

import akka.typed.javadsl.ActorContext;
import csw.services.logging.javadsl.JCommonComponentLoggerMutableActor;

//#common-component-logger-mutable-actor
//JExampleLoggerMutableActor is used for mutable actor java class only
public abstract class JExampleLoggerMutableActor extends JCommonComponentLoggerMutableActor<Object> {
    public JExampleLoggerMutableActor(ActorContext<Object> ctx) {
        super(ctx);
    }

    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#common-component-logger-mutable-actor