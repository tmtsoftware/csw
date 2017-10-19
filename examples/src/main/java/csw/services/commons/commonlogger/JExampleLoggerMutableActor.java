package csw.services.commons.commonlogger;

import akka.typed.javadsl.ActorContext;
import csw.services.logging.internal.LogControlMessages;
import csw.services.logging.javadsl.JCommonComponentLoggerMutableActor;

//#jcomponent-logger-mutable-actor
//JExampleLoggerMutableActor is used for mutable actor java class only
public abstract class JExampleLoggerMutableActor extends JCommonComponentLoggerMutableActor<LogControlMessages> {
    public JExampleLoggerMutableActor(ActorContext<LogControlMessages> ctx) {
        super(ctx);
    }

    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#jcomponent-logger-mutable-actor