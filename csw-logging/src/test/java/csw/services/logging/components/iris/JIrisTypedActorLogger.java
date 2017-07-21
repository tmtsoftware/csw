package csw.services.logging.components.iris;

import akka.typed.javadsl.ActorContext;
import csw.services.logging.javadsl.JComponentLoggerTypedActor;
//TODO: implement java version for LogCommand


abstract public class JIrisTypedActorLogger<T> extends JComponentLoggerTypedActor<T> {

    public static String NAME = "jIRISTyped";

    public JIrisTypedActorLogger(ActorContext actorContext) {
        super(actorContext);
    }

    @Override
    public String componentName() {
        return NAME;
    }
}